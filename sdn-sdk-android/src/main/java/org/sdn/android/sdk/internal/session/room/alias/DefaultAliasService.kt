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

package org.sdn.android.sdk.internal.session.room.alias

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.sdn.android.sdk.api.session.room.alias.AliasService

internal class DefaultAliasService @AssistedInject constructor(
        @Assisted private val roomId: String,
        private val getRoomLocalAliasesTask: GetRoomLocalAliasesTask,
        private val addRoomAliasTask: AddRoomAliasTask
) : AliasService {

    @AssistedFactory
    interface Factory {
        fun create(roomId: String): DefaultAliasService
    }

    override suspend fun getRoomAliases(): List<String> {
        return getRoomLocalAliasesTask.execute(GetRoomLocalAliasesTask.Params(roomId))
    }

    override suspend fun addAlias(aliasLocalPart: String) {
        addRoomAliasTask.execute(AddRoomAliasTask.Params(roomId, aliasLocalPart))
    }
}
