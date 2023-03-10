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

package org.sdn.android.sdk.api.session.room.send

import androidx.lifecycle.LiveData
import org.sdn.android.sdk.api.util.Optional

interface DraftService {

    /**
     * Save or update a draft to the room.
     */
    suspend fun saveDraft(draft: UserDraft)

    /**
     * Delete the last draft, basically just after sending the message.
     */
    suspend fun deleteDraft()

    /**
     * Return the current draft or null.
     */
    fun getDraft(): UserDraft?

    /**
     * Return the current draft if any, as a live data.
     */
    fun getDraftLive(): LiveData<Optional<UserDraft>>
}
