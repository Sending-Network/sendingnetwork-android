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

package org.sdn.android.sdk.api.query

/**
 * Filter room by their tag.
 * @see [org.matrix.android.sdk.api.session.room.RoomSummaryQueryParams]
 * @see [org.matrix.android.sdk.api.session.room.model.tag.RoomTag]
 */
data class RoomTagQueryFilter(
        /**
         * Set to true to get the rooms which have the tag "m.favourite".
         */
        val isFavorite: Boolean?,
        /**
         * Set to true to get the rooms which have the tag "m.lowpriority".
         */
        val isLowPriority: Boolean?,
        /**
         * Set to true to get the rooms which have the tag "m.server_notice".
         */
        val isServerNotice: Boolean?,
)
