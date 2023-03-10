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

package org.sdn.android.sdk.api.session.room.model.call

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * This event is sent by the callee when they wish to answer the call.
 */
@JsonClass(generateAdapter = true)
data class CallAssertedIdentityContent(
        /**
         * Required. The ID of the call this event relates to.
         */
        @Json(name = "call_id") override val callId: String,
        /**
         * Required. ID to let user identify remote echo of their own events
         */
        @Json(name = "party_id") override val partyId: String? = null,
        /**
         * Required. The version of the VoIP specification this messages adheres to.
         */
        @Json(name = "version") override val version: String?,

        /**
         * Optional. Used to inform the transferee who they're now speaking to.
         */
        @Json(name = "asserted_identity") val assertedIdentity: AssertedIdentity? = null
) : CallSignalingContent {

    /**
     *  A user ID may be included if relevant, but unlike target_user, it is purely informational.
     *  The asserted identity may not represent a matrix user at all,
     *  in which case just a display_name may be given, or a perhaps a display_name and avatar_url.
     */
    @JsonClass(generateAdapter = true)
    data class AssertedIdentity(
            @Json(name = "id") val id: String? = null,
            @Json(name = "display_name") val displayName: String? = null,
            @Json(name = "avatar_url") val avatarUrl: String? = null
    )
}
