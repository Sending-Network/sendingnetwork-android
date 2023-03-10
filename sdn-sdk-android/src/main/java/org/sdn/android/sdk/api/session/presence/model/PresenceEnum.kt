/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.sdn.android.sdk.api.session.presence.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class PresenceEnum(val value: String) {
    @Json(name = "online")
    ONLINE("online"),

    @Json(name = "offline")
    OFFLINE("offline"),

    @Json(name = "unavailable")
    UNAVAILABLE("unavailable"),

    @Json(name = "org.matrix.msc3026.busy")
    BUSY("busy");

    companion object {
        fun from(s: String): PresenceEnum? = values().find { it.value == s }
    }
}
