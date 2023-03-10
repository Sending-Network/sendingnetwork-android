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

package org.sdn.android.sdk.api.session.room.model.create

import org.sdn.android.sdk.api.session.events.model.EventType
import org.sdn.android.sdk.api.session.events.model.toContent
import org.sdn.android.sdk.api.session.node.NodeCapabilities
import org.sdn.android.sdk.api.session.room.model.GuestAccess
import org.sdn.android.sdk.api.session.room.model.RoomHistoryVisibility
import org.sdn.android.sdk.api.session.room.model.RoomJoinRules
import org.sdn.android.sdk.api.session.room.model.RoomJoinRulesAllowEntry
import org.sdn.android.sdk.api.session.room.model.RoomJoinRulesContent

interface RoomFeaturePreset {

    fun updateRoomParams(params: CreateRoomParams)

    fun setupInitialStates(): List<CreateRoomStateEvent>?
}

class RestrictedRoomPreset(val nodeCapabilities: NodeCapabilities, val restrictedList: List<RoomJoinRulesAllowEntry>) : RoomFeaturePreset {

    override fun updateRoomParams(params: CreateRoomParams) {
        params.historyVisibility = params.historyVisibility ?: RoomHistoryVisibility.SHARED
        params.guestAccess = params.guestAccess ?: GuestAccess.Forbidden
        params.roomVersion = nodeCapabilities.versionOverrideForFeature(NodeCapabilities.ROOM_CAP_RESTRICTED)
    }

    override fun setupInitialStates(): List<CreateRoomStateEvent> {
        return listOf(
                CreateRoomStateEvent(
                        type = EventType.STATE_ROOM_JOIN_RULES,
                        stateKey = "",
                        content = RoomJoinRulesContent(
                                joinRulesStr = RoomJoinRules.RESTRICTED.value,
                                allowList = restrictedList
                        ).toContent()
                )
        )
    }
}
