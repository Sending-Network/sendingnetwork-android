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

package org.sdn.android.sdk.api.auth.data

import org.sdn.android.sdk.api.auth.LoginType

/**
 * This data class holds necessary data to open a session.
 * You don't have to manually instantiate it.
 */
data class SessionParams(
    /**
         * Please consider using shortcuts instead.
         */
        val credentials: Credentials,

    /**
         * Please consider using shortcuts instead.
         */
        val edgeNodeConnectionConfig: EdgeNodeConnectionConfig,

    /**
         * Set to false if the current token is not valid anymore. Application should not have to use this info.
         */
        val isTokenValid: Boolean,

    /**
         * The authentication method that was used to create the session.
         */
        val loginType: LoginType,
) {
    /*
     * Shortcuts. Usually the application should only need to use these shortcuts
     */

    /**
     * The userId of the session (Ex: "@user:domain.org").
     */
    val userId = credentials.userId

    /**
     * The deviceId of the session (Ex: "ABCDEFGH").
     */
    val deviceId = credentials.deviceId

    /**
     * The node Url entered by the user during the login phase.
     */
    val nodeUrl = edgeNodeConnectionConfig.homeServerUri.toString()

    /**
     * The current homeserver Url for client-server API. It can be different that the homeserver url entered
     * during login phase, because a redirection may have occurred
     */
    val nodeUrlBase = edgeNodeConnectionConfig.homeServerUriBase.toString()

    /**
     * The current homeserver host, using what has been entered by the user during login phase.
     */
    val homeServerHost = edgeNodeConnectionConfig.homeServerUri.host

    /**
     * The default identity server url if any, returned by the homeserver during login phase.
     */
    val defaultIdentityServerUrl = edgeNodeConnectionConfig.identityServerUri?.toString()
}
