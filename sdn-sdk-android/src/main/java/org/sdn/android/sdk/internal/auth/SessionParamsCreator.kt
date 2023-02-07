/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sdn.android.sdk.internal.auth

import android.net.Uri
import org.sdn.android.sdk.api.auth.LoginType
import org.sdn.android.sdk.api.auth.data.Credentials
import org.sdn.android.sdk.api.auth.data.EdgeNodeConnectionConfig
import org.sdn.android.sdk.api.auth.data.SessionParams
import org.sdn.android.sdk.api.extensions.tryOrNull
import timber.log.Timber
import javax.inject.Inject

internal interface SessionParamsCreator {

    suspend fun create(
        credentials: Credentials,
        edgeNodeConnectionConfig: EdgeNodeConnectionConfig,
        loginType: LoginType,
    ): SessionParams
}

internal class DefaultSessionParamsCreator @Inject constructor(
        private val isValidClientServerApiTask: IsValidClientServerApiTask
) : SessionParamsCreator {

    override suspend fun create(
        credentials: Credentials,
        edgeNodeConnectionConfig: EdgeNodeConnectionConfig,
        loginType: LoginType,
    ) = SessionParams(
            credentials = credentials,
            edgeNodeConnectionConfig = edgeNodeConnectionConfig.overrideWithCredentials(credentials),
            isTokenValid = true,
            loginType = loginType,
    )

    private suspend fun EdgeNodeConnectionConfig.overrideWithCredentials(credentials: Credentials) = copy(
            homeServerUriBase = credentials.getHomeServerUri(this) ?: homeServerUriBase,
            identityServerUri = credentials.getIdentityServerUri() ?: identityServerUri
    )

    private suspend fun Credentials.getHomeServerUri(edgeNodeConnectionConfig: EdgeNodeConnectionConfig) =
            discoveryInformation?.homeServer?.baseURL
                    ?.trim { it == '/' }
                    ?.takeIf { it.isNotBlank() }
                    // It can be the same value, so in this case, do not check again the validity
                    ?.takeIf { it != edgeNodeConnectionConfig.homeServerUriBase.toString() }
                    ?.also { Timber.d("Overriding homeserver url to $it (will check if valid)") }
                    ?.let { Uri.parse(it) }
                    ?.takeIf { validateUri(it, edgeNodeConnectionConfig) }

    private suspend fun validateUri(uri: Uri, edgeNodeConnectionConfig: EdgeNodeConnectionConfig) =
            // Validate the URL, if the configuration is wrong server side, do not override
            tryOrNull {
                performClientServerApiValidation(uri, edgeNodeConnectionConfig)
            } ?: true // In case of other error (no network, etc.), consider it is valid...

    private suspend fun performClientServerApiValidation(uri: Uri, edgeNodeConnectionConfig: EdgeNodeConnectionConfig) =
            isValidClientServerApiTask.execute(
                    IsValidClientServerApiTask.Params(edgeNodeConnectionConfig.copy(homeServerUriBase = uri))
            ).also { Timber.d("Overriding homeserver url: $it") }

    private fun Credentials.getIdentityServerUri() = discoveryInformation?.identityServer?.baseURL
            ?.trim { it == '/' }
            ?.takeIf { it.isNotBlank() }
            ?.also { Timber.d("Overriding identity server url to $it") }
            ?.let { Uri.parse(it) }
}
