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

package org.sdn.android.sdk.api.session.room.model.call

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Sent by either party to signal their termination of the call. This can be sent either once
 * the call has been established or before to abort the call.
 */
@JsonClass(generateAdapter = true)
data class CallHangupContent(
        /**
         * Required. The ID of the call this event relates to.
         */
        @Json(name = "call_id") override val callId: String,
        /**
         * Required. ID to let user identify remote echo of their own events
         */
        @Json(name = "party_id") override val partyId: String? = null,
        /**
         * Required. The version of the VoIP specification this message adheres to.
         */
        @Json(name = "version") override val version: String?,
        /**
         * Optional error reason for the hangup. This should not be provided when the user naturally ends or rejects the call.
         * When there was an error in the call negotiation, this should be `ice_failed` for when ICE negotiation fails
         * or `invite_timeout` for when the other party did not answer in time.
         * One of: ["ice_failed", "invite_timeout"]
         */
        @Json(name = "reason") val reason: EndCallReason? = null
) : CallSignalingContent
