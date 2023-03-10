/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.sdn.android.sdk.internal.session.room.create

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.sdn.android.sdk.api.session.identity.ThreePid
import org.sdn.android.sdk.api.session.room.model.Membership

/**
 * Class representing the EventType.LOCAL_STATE_ROOM_THIRD_PARTY_INVITE state event content
 * This class is only used to store the third party invite data of a local room.
 */
@JsonClass(generateAdapter = true)
internal data class LocalRoomThirdPartyInviteContent(
        @Json(name = "membership") val membership: Membership,
        @Json(name = "displayname") val displayName: String? = null,
        @Json(name = "is_direct") val isDirect: Boolean = false,
        @Json(name = "third_party_invite") val thirdPartyInvite: ThreePid? = null,
)
