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

package org.sdn.android.sdk.api.session.sync.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.sdn.android.sdk.api.session.events.model.Event

// RoomSyncTimeline represents the timeline of messages and state changes for a room during server sync v2.
@JsonClass(generateAdapter = true)
data class RoomSyncTimeline(

        /**
         * List of events (array of Event).
         */
        @Json(name = "events") val events: List<Event>? = null,

        /**
         * Boolean which tells whether there are more events on the server.
         */
        @Json(name = "limited") val limited: Boolean = false,

        /**
         * If the batch was limited then this is a token that can be supplied to the server to retrieve more events.
         */
        @Json(name = "prev_batch") val prevToken: String? = null
)
