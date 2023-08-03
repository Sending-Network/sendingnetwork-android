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

package org.sdn.android.sdk.internal.auth.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.sdn.android.sdk.api.auth.data.LoginFlowTypes

@JsonClass(generateAdapter = true)
internal data class DidLoginParams(
        @Json(name = "type") override val type: String = LoginFlowTypes.DID,
        @Json(name = "updated") val updated: String,
        @Json(name = "random_server") val randomServer: String,
        @Json(name = "initial_device_display_name") override val deviceDisplayName: String? = null,
        @Json(name = "device_id") override val deviceId: String? = null,
        @Json(name = "identifier") val identifier: Identifier
) : LoginParams

@JsonClass(generateAdapter = true)
internal data class Identifier (
        @Json(name = "address")  val address: String,
        @Json(name = "did")  val did: String,
        @Json(name = "token")  val token: String,
        @Json(name = "app_token")  val appToken: String,
)