/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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

@JsonClass(generateAdapter = false)
enum class EndCallReason {
    @Json(name = "ice_failed")
    ICE_FAILED,

    @Json(name = "ice_timeout")
    ICE_TIMEOUT,

    @Json(name = "user_hangup")
    USER_HANGUP,

    @Json(name = "replaced")
    REPLACED,

    @Json(name = "user_media_failed")
    USER_MEDIA_FAILED,

    @Json(name = "invite_timeout")
    INVITE_TIMEOUT,

    @Json(name = "unknown_error")
    UNKWOWN_ERROR,

    @Json(name = "user_busy")
    USER_BUSY,

    @Json(name = "answered_elsewhere")
    ANSWERED_ELSEWHERE
}
