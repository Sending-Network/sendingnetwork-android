/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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
 *
 */

package org.sdn.android.sdk.api.session.room.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RoomJoinRulesAllowEntry(
        /**
         * The room ID to check the membership of.
         */
        @Json(name = "room_id") val roomId: String?,
        /**
         * "m.room_membership" to describe that we are allowing access via room membership. Future MSCs may define other types.
         */
        @Json(name = "type") val type: String?
) {
    companion object {
        fun restrictedToRoom(roomId: String) = RoomJoinRulesAllowEntry(roomId, "m.room_membership")
    }
}
