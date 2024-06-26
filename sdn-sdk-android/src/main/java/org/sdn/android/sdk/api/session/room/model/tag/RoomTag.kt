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

package org.sdn.android.sdk.api.session.room.model.tag

data class RoomTag(
        val name: String,
        val order: Double?
) {

    companion object {
        const val ROOM_TAG_FAVOURITE = "m.favourite"
        const val ROOM_TAG_LOW_PRIORITY = "m.lowpriority"
        const val ROOM_TAG_SERVER_NOTICE = "m.server_notice"
        const val ROOM_TAG_INVISIBLE = "m.invisible" // room property is "isHiddenFromUser"
    }
}
