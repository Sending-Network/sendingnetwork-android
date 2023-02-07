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
package org.sdn.android.sdk.internal.session.profile

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class AddEmailBody(
        /**
         * Required. A unique string generated by the client, and used to identify the validation attempt.
         * It must be a string consisting of the characters [0-9a-zA-Z.=_-]. Its length must not exceed
         * 255 characters and it must not be empty.
         */
        @Json(name = "client_secret")
        val clientSecret: String,

        /**
         * Required. The email address to validate.
         */
        @Json(name = "email")
        val email: String,

        /**
         * Required. The server will only send an email if the send_attempt is a number greater than the most
         * recent one which it has seen, scoped to that email + client_secret pair. This is to avoid repeatedly
         * sending the same email in the case of request retries between the POSTing user and the identity server.
         * The client should increment this value if they desire a new email (e.g. a reminder) to be sent.
         * If they do not, the server should respond with success but not resend the email.
         */
        @Json(name = "send_attempt")
        val sendAttempt: Int
)
