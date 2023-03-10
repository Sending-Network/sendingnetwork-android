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

package org.sdn.android.sdk.internal.session.sync.model.accountdata

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.sdn.android.sdk.api.util.emptyJsonDict

@JsonClass(generateAdapter = true)
internal data class IgnoredUsersContent(
        /**
         * Required. The map of users to ignore. UserId -> empty object for future enhancement
         */
        @Json(name = "ignored_users") val ignoredUsers: Map<String, Any>
) {

    companion object {
        fun createWithUserIds(userIds: List<String>): IgnoredUsersContent {
            return IgnoredUsersContent(
                    ignoredUsers = userIds.associateWith { emptyJsonDict }
            )
        }
    }
}
