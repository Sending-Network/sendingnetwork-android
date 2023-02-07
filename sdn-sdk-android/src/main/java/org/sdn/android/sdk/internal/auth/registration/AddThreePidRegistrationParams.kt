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

package org.sdn.android.sdk.internal.auth.registration

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.sdn.android.sdk.api.auth.registration.RegisterThreePid

/**
 * Add a three Pid during authentication.
 */
@JsonClass(generateAdapter = true)
internal data class AddThreePidRegistrationParams(
        /**
         * Required. A unique string generated by the client, and used to identify the validation attempt.
         * It must be a string consisting of the characters [0-9a-zA-Z.=_-]. Its length must not exceed 255 characters and it must not be empty.
         */
        @Json(name = "client_secret")
        val clientSecret: String,

        /**
         * Required. The server will only send an email if the send_attempt is a number greater than the most recent one which it has seen,
         * scoped to that email + client_secret pair. This is to avoid repeatedly sending the same email in the case of request retries between
         * the POSTing user and the identity server. The client should increment this value if they desire a new email (e.g. a reminder) to be sent.
         * If they do not, the server should respond with success but not resend the email.
         */
        @Json(name = "send_attempt")
        val sendAttempt: Int,

        /**
         * Optional. When the validation is completed, the identity server will redirect the user to this URL. This option is ignored when
         * submitting 3PID validation information through a POST request.
         */
        @Json(name = "next_link")
        val nextLink: String? = null,

        /**
         * Required. The hostname of the identity server to communicate with. May optionally include a port.
         * This parameter is ignored when the homeserver handles 3PID verification.
         */
        @Json(name = "id_server")
        val idServer: String? = null,

        /* ==========================================================================================
         * For emails
         * ========================================================================================== */

        /**
         * Required. The email address to validate.
         */
        @Json(name = "email")
        val email: String? = null,

        /* ==========================================================================================
         * For Msisdn
         * ========================================================================================== */

        /**
         * Required. The two-letter uppercase ISO country code that the number in phone_number should be parsed as if it were dialled from.
         */
        @Json(name = "country")
        val countryCode: String? = null,

        /**
         * Required. The phone number to validate.
         */
        @Json(name = "phone_number")
        val msisdn: String? = null
) {
    companion object {
        fun from(params: RegisterAddThreePidTask.Params): AddThreePidRegistrationParams {
            return when (params.threePid) {
                is RegisterThreePid.Email -> AddThreePidRegistrationParams(
                        email = params.threePid.email,
                        clientSecret = params.clientSecret,
                        sendAttempt = params.sendAttempt
                )
                is RegisterThreePid.Msisdn -> AddThreePidRegistrationParams(
                        msisdn = params.threePid.msisdn,
                        countryCode = params.threePid.countryCode,
                        clientSecret = params.clientSecret,
                        sendAttempt = params.sendAttempt
                )
            }
        }
    }
}
