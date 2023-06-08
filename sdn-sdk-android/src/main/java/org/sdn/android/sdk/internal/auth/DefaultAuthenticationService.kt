/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sdn.android.sdk.internal.auth

import android.net.Uri
import dagger.Lazy
import okhttp3.OkHttpClient
import org.sdn.android.sdk.api.MatrixPatterns
import org.sdn.android.sdk.api.MatrixPatterns.getServerName
import org.sdn.android.sdk.api.auth.AuthenticationService
import org.sdn.android.sdk.api.auth.LoginType
import org.sdn.android.sdk.api.auth.data.*
import org.sdn.android.sdk.api.auth.login.LoginWizard
import org.sdn.android.sdk.api.auth.registration.RegistrationWizard
import org.sdn.android.sdk.api.auth.wellknown.WellknownResult
import org.sdn.android.sdk.api.extensions.orFalse
import org.sdn.android.sdk.api.failure.Failure
import org.sdn.android.sdk.api.failure.MatrixIdFailure
import org.sdn.android.sdk.api.session.Session
import org.sdn.android.sdk.api.util.appendParamToUrl
import org.sdn.android.sdk.internal.SessionManager
import org.sdn.android.sdk.internal.auth.data.WebClientConfig
import org.sdn.android.sdk.internal.auth.db.PendingSessionData
import org.sdn.android.sdk.internal.auth.login.*
import org.sdn.android.sdk.internal.auth.login.DefaultLoginWizard
import org.sdn.android.sdk.internal.auth.login.DidLoginTask
import org.sdn.android.sdk.internal.auth.login.DirectLoginTask
import org.sdn.android.sdk.internal.auth.login.QrLoginTokenTask
import org.sdn.android.sdk.internal.auth.registration.DefaultRegistrationWizard
import org.sdn.android.sdk.internal.auth.version.Versions
import org.sdn.android.sdk.internal.auth.version.doesServerSupportLogoutDevices
import org.sdn.android.sdk.internal.auth.version.doesServerSupportQrCodeLogin
import org.sdn.android.sdk.internal.auth.version.isLoginAndRegistrationSupportedBySdk
import org.sdn.android.sdk.internal.auth.version.isSupportedBySdk
import org.sdn.android.sdk.internal.di.Unauthenticated
import org.sdn.android.sdk.internal.network.RetrofitFactory
import org.sdn.android.sdk.internal.network.executeRequest
import org.sdn.android.sdk.internal.network.httpclient.addSocketFactory
import org.sdn.android.sdk.internal.network.ssl.UnrecognizedCertificateException
import org.sdn.android.sdk.internal.wellknown.GetWellknownTask
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

internal class DefaultAuthenticationService @Inject constructor(
        @Unauthenticated
        private val okHttpClient: Lazy<OkHttpClient>,
        private val retrofitFactory: RetrofitFactory,
        private val sessionParamsStore: SessionParamsStore,
        private val sessionManager: SessionManager,
        private val sessionCreator: SessionCreator,
        private val pendingSessionStore: PendingSessionStore,
        private val getWellknownTask: GetWellknownTask,
        private val directLoginTask: DirectLoginTask,
        private val qrLoginTokenTask: QrLoginTokenTask,
        private val didListTask: DidListTask,
        private val didCreateTask: DidCreateTask,
        private val didSaveTask: DidSaveTask,
        private val didPreLoginTask: DidPreLoginTask,
        private val didLoginTask: DidLoginTask
) : AuthenticationService {

    private var pendingSessionData: PendingSessionData? = pendingSessionStore.getPendingSessionData()

    private var currentLoginWizard: LoginWizard? = null
    private var currentRegistrationWizard: RegistrationWizard? = null

    override fun hasAuthenticatedSessions(): Boolean {
        return sessionParamsStore.getLast() != null
    }

    override fun getLastAuthenticatedSession(): Session? {
        return sessionManager.getLastSession()
    }

    override suspend fun getLoginFlowOfSession(sessionId: String): LoginFlowResult {
        val homeServerConnectionConfig = sessionParamsStore.get(sessionId)?.edgeNodeConnectionConfig
                ?: throw IllegalStateException("Session not found")

        return getLoginFlow(homeServerConnectionConfig)
    }

    override fun getSsoUrl(redirectUrl: String, deviceId: String?, providerId: String?): String? {
        val homeServerUrlBase = getHomeServerUrlBase() ?: return null

        return buildString {
            append(homeServerUrlBase)
            append(SSO_REDIRECT_PATH)
            if (providerId != null) {
                append("/$providerId")
            }
            // Set the redirect url
            appendParamToUrl(SSO_REDIRECT_URL_PARAM, redirectUrl)
            deviceId?.takeIf { it.isNotBlank() }?.let {
                // But https://github.com/matrix-org/synapse/issues/5755
                appendParamToUrl("device_id", it)
            }
        }
    }

    override fun getFallbackUrl(forSignIn: Boolean, deviceId: String?): String? {
        val homeServerUrlBase = getHomeServerUrlBase() ?: return null

        return buildString {
            append(homeServerUrlBase)
            if (forSignIn) {
                append(LOGIN_FALLBACK_PATH)
                deviceId?.takeIf { it.isNotBlank() }?.let {
                    // But https://github.com/matrix-org/synapse/issues/5755
                    appendParamToUrl("device_id", it)
                }
            } else {
                // For sign up
                append(REGISTER_FALLBACK_PATH)
            }
        }
    }

    private fun getHomeServerUrlBase(): String? {
        return pendingSessionData
                ?.edgeNodeConnectionConfig
                ?.homeServerUriBase
                ?.toString()
                ?.trim { it == '/' }
    }

    override suspend fun getLoginFlow(edgeNodeConnectionConfig: EdgeNodeConnectionConfig): LoginFlowResult {
        val result = runCatching {
            getLoginFlowInternal(edgeNodeConnectionConfig)
        }
        return result.fold(
                {
                    // The homeserver exists and up to date, keep the config
                    // Homeserver url may have been changed, if it was a Web client url
                    val alteredHomeServerConnectionConfig = edgeNodeConnectionConfig.copy(
                            homeServerUriBase = Uri.parse(it.homeServerUrl)
                    )

                    pendingSessionData = PendingSessionData(alteredHomeServerConnectionConfig)
                            .also { data -> pendingSessionStore.savePendingSessionData(data) }
                    it
                },
                {
                    if (it is UnrecognizedCertificateException) {
                        throw Failure.UnrecognizedCertificateFailure(edgeNodeConnectionConfig.homeServerUriBase.toString(), it.fingerprint)
                    } else {
                        throw it
                    }
                }
        )
    }

    private suspend fun getLoginFlowInternal(edgeNodeConnectionConfig: EdgeNodeConnectionConfig): LoginFlowResult {
        val authAPI = buildAuthAPI(edgeNodeConnectionConfig)

        // First check if there is a well-known file
        return try {
            getWellknownLoginFlowInternal(edgeNodeConnectionConfig)
        } catch (failure: Throwable) {
            if (failure is Failure.OtherServerError &&
                    failure.httpCode == HttpsURLConnection.HTTP_NOT_FOUND /* 404 */) {
                // 404, no well-known data, try direct access to the API
                // First check the homeserver version
                return runCatching {
                    executeRequest(null) {
                        authAPI.versions()
                    }
                }
                        .map { versions ->
                            // Ok, it seems that the homeserver url is valid
                            getLoginFlowResult(authAPI, versions, edgeNodeConnectionConfig.homeServerUriBase.toString())
                        }
                        .fold(
                                {
                                    it
                                },
                                {
                                    if (it is Failure.OtherServerError &&
                                            it.httpCode == HttpsURLConnection.HTTP_NOT_FOUND /* 404 */) {
                                        // It's maybe a Web client url?
                                        getWebClientDomainLoginFlowInternal(edgeNodeConnectionConfig)
                                    } else {
                                        throw it
                                    }
                                }
                        )
            } else {
                throw failure
            }
        }
    }

    private suspend fun getWebClientDomainLoginFlowInternal(edgeNodeConnectionConfig: EdgeNodeConnectionConfig): LoginFlowResult {
        val authAPI = buildAuthAPI(edgeNodeConnectionConfig)

        val domain = edgeNodeConnectionConfig.homeServerUri.host
                ?: return getWebClientLoginFlowInternal(edgeNodeConnectionConfig)

        // Ok, try to get the config.domain.json file of a Web client
        return runCatching {
            executeRequest(null) {
                authAPI.getWebClientConfigDomain(domain)
            }
        }
                .map { webClientConfig ->
                    onWebClientConfigRetrieved(edgeNodeConnectionConfig, webClientConfig)
                }
                .fold(
                        {
                            it
                        },
                        {
                            if (it is Failure.OtherServerError &&
                                    it.httpCode == HttpsURLConnection.HTTP_NOT_FOUND /* 404 */) {
                                // Try with config.json
                                getWebClientLoginFlowInternal(edgeNodeConnectionConfig)
                            } else {
                                throw it
                            }
                        }
                )
    }

    private suspend fun getWebClientLoginFlowInternal(edgeNodeConnectionConfig: EdgeNodeConnectionConfig): LoginFlowResult {
        val authAPI = buildAuthAPI(edgeNodeConnectionConfig)

        // Ok, try to get the config.json file of a Web client
        return executeRequest(null) {
            authAPI.getWebClientConfig()
        }
                .let { webClientConfig ->
                    onWebClientConfigRetrieved(edgeNodeConnectionConfig, webClientConfig)
                }
    }

    private suspend fun onWebClientConfigRetrieved(edgeNodeConnectionConfig: EdgeNodeConnectionConfig, webClientConfig: WebClientConfig): LoginFlowResult {
        val defaultHomeServerUrl = webClientConfig.getPreferredHomeServerUrl()
        if (defaultHomeServerUrl?.isNotEmpty() == true) {
            // Ok, good sign, we got a default hs url
            val newHomeServerConnectionConfig = edgeNodeConnectionConfig.copy(
                    homeServerUriBase = Uri.parse(defaultHomeServerUrl)
            )

            val newAuthAPI = buildAuthAPI(newHomeServerConnectionConfig)

            val versions = executeRequest(null) {
                newAuthAPI.versions()
            }

            return getLoginFlowResult(newAuthAPI, versions, defaultHomeServerUrl)
        } else {
            // Config exists, but there is no default homeserver url (ex: https://riot.im/app)
            throw Failure.OtherServerError("", HttpsURLConnection.HTTP_NOT_FOUND /* 404 */)
        }
    }

    private suspend fun getWellknownLoginFlowInternal(edgeNodeConnectionConfig: EdgeNodeConnectionConfig): LoginFlowResult {
        val domain = edgeNodeConnectionConfig.homeServerUri.host
                ?: throw Failure.OtherServerError("", HttpsURLConnection.HTTP_NOT_FOUND /* 404 */)

        val wellknownResult = getWellknownTask.execute(GetWellknownTask.Params(domain, edgeNodeConnectionConfig))

        return when (wellknownResult) {
            is WellknownResult.Prompt -> {
                val newHomeServerConnectionConfig = edgeNodeConnectionConfig.copy(
                        homeServerUriBase = Uri.parse(wellknownResult.homeServerUrl),
                        identityServerUri = wellknownResult.identityServerUrl?.let { Uri.parse(it) } ?: edgeNodeConnectionConfig.identityServerUri
                )

                val newAuthAPI = buildAuthAPI(newHomeServerConnectionConfig)

                val versions = executeRequest(null) {
                    newAuthAPI.versions()
                }

                getLoginFlowResult(newAuthAPI, versions, wellknownResult.homeServerUrl)
            }
            else -> throw Failure.OtherServerError("", HttpsURLConnection.HTTP_NOT_FOUND /* 404 */)
        }
    }

    private suspend fun getLoginFlowResult(authAPI: AuthAPI, versions: Versions, homeServerUrl: String): LoginFlowResult {
        // Get the login flow
        val loginFlowResponse = executeRequest(null) {
            authAPI.getLoginFlows()
        }
        return LoginFlowResult(
                supportedLoginTypes = loginFlowResponse.flows.orEmpty().mapNotNull { it.type },
                ssoIdentityProviders = loginFlowResponse.flows.orEmpty().firstOrNull { it.type == LoginFlowTypes.SSO }?.ssoIdentityProvider,
                isLoginAndRegistrationSupported = versions.isLoginAndRegistrationSupportedBySdk(),
                homeServerUrl = homeServerUrl,
                isOutdatedHomeserver = !versions.isSupportedBySdk(),
                isLogoutDevicesSupported = versions.doesServerSupportLogoutDevices()
        )
    }

    override fun getRegistrationWizard(): RegistrationWizard {
        return currentRegistrationWizard
                ?: let {
                    pendingSessionData?.edgeNodeConnectionConfig?.let {
                        DefaultRegistrationWizard(
                                buildAuthAPI(it),
                                sessionCreator,
                                pendingSessionStore
                        ).also {
                            currentRegistrationWizard = it
                        }
                    } ?: error("Please call getLoginFlow() with success first")
                }
    }

    override fun isRegistrationStarted() = currentRegistrationWizard?.isRegistrationStarted() == true

    override fun getLoginWizard(): LoginWizard {
        return currentLoginWizard
                ?: let {
                    pendingSessionData?.edgeNodeConnectionConfig?.let {
                        DefaultLoginWizard(
                                buildAuthAPI(it),
                                sessionCreator,
                                pendingSessionStore
                        ).also {
                            currentLoginWizard = it
                        }
                    } ?: error("Please call getLoginFlow() with success first")
                }
    }

    override suspend fun cancelPendingLoginOrRegistration() {
        currentLoginWizard = null
        currentRegistrationWizard = null

        // Keep only the home sever config
        // Update the local pendingSessionData synchronously
        pendingSessionData = pendingSessionData?.edgeNodeConnectionConfig
                ?.let { PendingSessionData(it) }
                .also {
                    if (it == null) {
                        // Should not happen
                        pendingSessionStore.delete()
                    } else {
                        pendingSessionStore.savePendingSessionData(it)
                    }
                }
    }

    override suspend fun reset() {
        currentLoginWizard = null
        currentRegistrationWizard = null

        pendingSessionData = null

        pendingSessionStore.delete()
    }

    override suspend fun createSessionFromSso(
        edgeNodeConnectionConfig: EdgeNodeConnectionConfig,
        credentials: Credentials
    ): Session {
        return sessionCreator.createSession(credentials, edgeNodeConnectionConfig, LoginType.SSO)
    }

    override suspend fun getWellKnownData(
        userId: String,
        edgeNodeConnectionConfig: EdgeNodeConnectionConfig?
    ): WellknownResult {
        if (!MatrixPatterns.isUserId(userId)) {
            throw MatrixIdFailure.InvalidMatrixId
        }

        return getWellknownTask.execute(
                GetWellknownTask.Params(
                        domain = userId.getServerName().substringBeforeLast(":"),
                        edgeNodeConnectionConfig = edgeNodeConnectionConfig.orWellKnownDefaults()
                )
        )
    }

    private fun EdgeNodeConnectionConfig?.orWellKnownDefaults() = this ?: EdgeNodeConnectionConfig.Builder()
            // server uri is ignored when doing a wellknown lookup as we use the matrix id domain instead
            .withNodeUri("https://dummy.org")
            .build()

    override suspend fun directAuthentication(
        edgeNodeConnectionConfig: EdgeNodeConnectionConfig,
        userId: String,
        password: String,
        initialDeviceName: String,
        deviceId: String?
    ): Session {
        return directLoginTask.execute(
                DirectLoginTask.Params(
                        edgeNodeConnectionConfig = edgeNodeConnectionConfig,
                        userId = userId,
                        password = password,
                        deviceName = initialDeviceName,
                        deviceId = deviceId
                )
        )
    }

    override suspend fun didList(
        edgeNodeConnectionConfig: EdgeNodeConnectionConfig,
        address: String,
    ): DidListResp {
        return didListTask.execute(
            DidListTask.Params(
                edgeNodeConnectionConfig = edgeNodeConnectionConfig,
                address = address
            )
        )
    }

    override suspend fun didCreate(
        edgeNodeConnectionConfig: EdgeNodeConnectionConfig,
        address: String,
    ): DidCreateResp {
        return didCreateTask.execute(
            DidCreateTask.Params(
                edgeNodeConnectionConfig = edgeNodeConnectionConfig,
                address = address
            )
        )
    }

    override suspend fun didSave(
        edgeNodeConnectionConfig: EdgeNodeConnectionConfig,
        did: String,
        signature: String,
        operation: String,
        ids: List<String>?,
        address: String?,
        updated: String,
    ): DidSaveResp {
        return didSaveTask.execute(
            DidSaveTask.Params(
                edgeNodeConnectionConfig = edgeNodeConnectionConfig,
                did = did,
                signature = signature,
                operation = operation,
                ids = ids,
                address = address,
                updated = updated,
            )
        )
    }

    override suspend fun didPreLogin(
        edgeNodeConnectionConfig: EdgeNodeConnectionConfig,
        address: String,
    ): LoginDidMsg {
        return didPreLoginTask.execute(
            DidPreLoginTask.Params(
                edgeNodeConnectionConfig = edgeNodeConnectionConfig,
                address = address
            )
        )
    }

    override suspend fun didLogin(
        edgeNodeConnectionConfig: EdgeNodeConnectionConfig,
        did: String,
        nonce: String,
        updateTime: String,
        token: String,
    ): Session {
        return didLoginTask.execute(
            DidLoginTask.Params(
                edgeNodeConnectionConfig = edgeNodeConnectionConfig,
                did = did,
                nonce = nonce,
                updated = updateTime,
                token = token,
                deviceName = "",
                deviceId = null
            )
        )
    }

    override suspend fun isQrLoginSupported(edgeNodeConnectionConfig: EdgeNodeConnectionConfig): Boolean {
        val authAPI = buildAuthAPI(edgeNodeConnectionConfig)
        val versions = runCatching {
            executeRequest(null) {
                authAPI.versions()
            }
        }
        return if (versions.isSuccess) {
            versions.getOrNull()?.doesServerSupportQrCodeLogin().orFalse()
        } else {
            false
        }
    }

    override suspend fun loginUsingQrLoginToken(
        edgeNodeConnectionConfig: EdgeNodeConnectionConfig,
        loginToken: String,
        initialDeviceName: String?,
        deviceId: String?,
    ): Session {
        return qrLoginTokenTask.execute(
                QrLoginTokenTask.Params(
                        edgeNodeConnectionConfig = edgeNodeConnectionConfig,
                        loginToken = loginToken,
                        deviceName = initialDeviceName,
                        deviceId = deviceId
                )
        )
    }

    private fun buildAuthAPI(edgeNodeConnectionConfig: EdgeNodeConnectionConfig): AuthAPI {
        val retrofit = retrofitFactory.create(buildClient(edgeNodeConnectionConfig), edgeNodeConnectionConfig.homeServerUriBase.toString())
        return retrofit.create(AuthAPI::class.java)
    }

    private fun buildClient(edgeNodeConnectionConfig: EdgeNodeConnectionConfig): OkHttpClient {
        return okHttpClient.get()
                .newBuilder()
                .addSocketFactory(edgeNodeConnectionConfig)
                .build()
    }
}
