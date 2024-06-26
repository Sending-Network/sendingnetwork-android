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

package org.sdn.android.sdk.api.session.user.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ContactInfo (
        @Json(name = "contact_id") val contactId: String,
        @Json(name = "displayname") val displayName: String?,
        @Json(name = "avatar_url") val avatarUrl: String?,
        @Json(name = "wallet_address") val walletAddress: String?,
        @Json(name = "tags") val tags: List<String>?,
)

@JsonClass(generateAdapter = true)
data class ContactsResponse(
        @Json(name = "people") val people: List<ContactInfo>? = null
)
