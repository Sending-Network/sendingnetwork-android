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

package org.sdn.android.sdk.internal.session.room.uploads

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.sdn.android.sdk.api.session.crypto.CryptoService
import org.sdn.android.sdk.api.session.room.uploads.GetUploadsResult
import org.sdn.android.sdk.api.session.room.uploads.UploadsService

internal class DefaultUploadsService @AssistedInject constructor(
        @Assisted private val roomId: String,
        private val getUploadsTask: GetUploadsTask,
        private val cryptoService: CryptoService
) : UploadsService {

    @AssistedFactory
    interface Factory {
        fun create(roomId: String): DefaultUploadsService
    }

    override suspend fun getUploads(numberOfEvents: Int, since: String?): GetUploadsResult {
        return getUploadsTask.execute(GetUploadsTask.Params(roomId, cryptoService.isRoomEncrypted(roomId), numberOfEvents, since))
    }
}
