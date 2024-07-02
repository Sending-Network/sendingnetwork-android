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

package org.sdn.android.sdk.api.auth

import org.sdn.android.sdk.api.auth.data.*
import org.sdn.android.sdk.api.auth.login.LoginWizard
import org.sdn.android.sdk.api.auth.registration.RegistrationWizard
import org.sdn.android.sdk.api.auth.wellknown.WellknownResult
import org.sdn.android.sdk.api.session.Session

/**
 * This interface defines methods to authenticate or to create an account to a matrix server.
 */
interface AuthenticationService {

    /**
     * Request the supported login flows for this homeserver.
     * This is the first method to call to be able to get a wizard to login or to create an account.
     * @param edgeNodeConnectionConfig contains the homeserver URL to login to, a wellKnown lookup will be attempted.
     */
    suspend fun getLoginFlow(edgeNodeConnectionConfig: EdgeNodeConnectionConfig): LoginFlowResult

    /**
     * Request the supported login flows for the corresponding sessionId.
     */
    suspend fun getLoginFlowOfSession(sessionId: String): LoginFlowResult

    /**
     * Get a SSO url.
     */
    fun getSsoUrl(redirectUrl: String, deviceId: String?, providerId: String?): String?

    /**
     * Get the sign in or sign up fallback URL.
     */
    fun getFallbackUrl(forSignIn: Boolean, deviceId: String?): String?

    /**
     * Return a LoginWizard, to login to the homeserver. The login flow has to be retrieved first.
     *
     * See [LoginWizard] for more details
     */
    fun getLoginWizard(): LoginWizard

    /**
     * Return a RegistrationWizard, to create an matrix account on the homeserver. The login flow has to be retrieved first.
     *
     * See [RegistrationWizard] for more details.
     */
    fun getRegistrationWizard(): RegistrationWizard

    /**
     * True when login and password has been sent with success to the homeserver.
     */
    fun isRegistrationStarted(): Boolean

    /**
     * Cancel pending login or pending registration.
     */
    suspend fun cancelPendingLoginOrRegistration()

    /**
     * Reset all pending settings, including current HomeServerConnectionConfig.
     */
    suspend fun reset()

    /**
     * Check if there is an authenticated [Session].
     * @return true if there is at least one active session.
     */
    fun hasAuthenticatedSessions(): Boolean

    /**
     * Get the last authenticated [Session], if there is an active session.
     * @return the last active session if any, or null
     */
    fun getLastAuthenticatedSession(): Session?

    fun getAuthenticatedSession(sessionId: String): Session?

    /**
     * Create a session after a SSO successful login.
     */
    suspend fun createSessionFromSso(
        edgeNodeConnectionConfig: EdgeNodeConnectionConfig,
        credentials: Credentials
    ): Session

    /**
     * Perform a wellknown request, using the domain from the userId.
     */
    suspend fun getWellKnownData(
        userId: String,
        edgeNodeConnectionConfig: EdgeNodeConnectionConfig?
    ): WellknownResult

    /**
     * Authenticate with a userId and a password.
     * Usually call this after a successful call to getWellKnownData().
     * @param edgeNodeConnectionConfig the information about the homeserver and other configuration
     * @param userId the userId of the user
     * @param password the password of the account
     * @param initialDeviceName the initial device name
     * @param deviceId the device id, optional. If not provided or null, the server will generate one.
     */
    suspend fun directAuthentication(
        edgeNodeConnectionConfig: EdgeNodeConnectionConfig,
        userId: String,
        password: String,
        initialDeviceName: String,
        deviceId: String? = null
    ): Session

    suspend fun didList(
        edgeNodeConnectionConfig: EdgeNodeConnectionConfig,
        address: String,
    ): DidListResp

    suspend fun didCreate(
        edgeNodeConnectionConfig: EdgeNodeConnectionConfig,
        did: String,
    ): DidCreateResp

    suspend fun didSave(
        edgeNodeConnectionConfig: EdgeNodeConnectionConfig,
        did: String,
        signature: String,
        operation: String,
        ids: List<String>?,
        address: String?,
        updated: String,
    ): DidSaveResp

    suspend fun didPreLogin(
        edgeNodeConnectionConfig: EdgeNodeConnectionConfig,
        address: String,
    ): LoginDidMsg

    suspend fun didLogin(
        edgeNodeConnectionConfig: EdgeNodeConnectionConfig,
        address: String,
        did: String,
        nonce: String,
        updateTime: String,
        token: String,
        appToken: String? = null,
    ): Session

    /**
     * @param edgeNodeConnectionConfig the information about the homeserver and other configuration
     * Return true if qr code login is supported by the server, false otherwise.
     */
    suspend fun isQrLoginSupported(edgeNodeConnectionConfig: EdgeNodeConnectionConfig): Boolean

    /**
     * Authenticate using m.login.token method during sign in with QR code.
     * @param edgeNodeConnectionConfig the information about the homeserver and other configuration
     * @param loginToken the m.login.token
     * @param initialDeviceName the initial device name
     * @param deviceId the device id, optional. If not provided or null, the server will generate one.
     */
    suspend fun loginUsingQrLoginToken(
        edgeNodeConnectionConfig: EdgeNodeConnectionConfig,
        loginToken: String,
        initialDeviceName: String? = null,
        deviceId: String? = null
    ): Session
}
