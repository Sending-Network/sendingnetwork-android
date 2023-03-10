/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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
package org.sdn.android.sdk.api.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.sdn.android.sdk.api.auth.data.LoginFlowTypes

/**
 * This class provides the authentication data by using user and password.
 */
@JsonClass(generateAdapter = true)
data class TokenBasedAuth(

        /**
         * This is a session identifier that the client must pass back to the homeserver,
         * if one is provided, in subsequent attempts to authenticate in the same API call.
         */
        @Json(name = "session")
        override val session: String? = null,

        /**
         * A client may receive a login token via some external service, such as email or SMS.
         * Note that a login token is separate from an access token, the latter providing general authentication to various API endpoints.
         */
        @Json(name = "token")
        val token: String? = null,

        /**
         * The txn_id should be a random string generated by the client for the request.
         * The same txn_id should be used if retrying the request.
         * The txn_id may be used by the server to disallow other devices from using the token,
         * thus providing "single use" tokens while still allowing the device to retry the request.
         * This would be done by tying the token to the txn_id server side, as well as potentially invalidating
         * the token completely once the device has successfully logged in
         * (e.g. when we receive a request from the newly provisioned access_token).
         */
        @Json(name = "txn_id")
        val transactionId: String? = null,

        // registration information
        @Json(name = "type")
        val type: String? = LoginFlowTypes.TOKEN

) : UIABaseAuth {
    override fun hasAuthInfo() = token != null

    override fun copyWithSession(session: String) = this.copy(session = session)

    override fun asMap(): Map<String, *> = mapOf(
            "session" to session,
            "token" to token,
            "transactionId" to transactionId,
            "type" to type
    )
}
