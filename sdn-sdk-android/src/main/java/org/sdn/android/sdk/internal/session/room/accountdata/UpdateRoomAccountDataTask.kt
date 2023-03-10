/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.sdn.android.sdk.internal.session.room.accountdata

import org.sdn.android.sdk.api.util.JsonDict
import org.sdn.android.sdk.internal.di.UserId
import org.sdn.android.sdk.internal.network.GlobalErrorReceiver
import org.sdn.android.sdk.internal.network.executeRequest
import org.sdn.android.sdk.internal.session.room.RoomAPI
import org.sdn.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface UpdateRoomAccountDataTask : Task<UpdateRoomAccountDataTask.Params, Unit> {

    data class Params(
            val roomId: String,
            val type: String,
            val content: JsonDict
    )
}

internal class DefaultUpdateRoomAccountDataTask @Inject constructor(
        private val roomApi: RoomAPI,
        @UserId private val userId: String,
        private val globalErrorReceiver: GlobalErrorReceiver
) : UpdateRoomAccountDataTask {

    override suspend fun execute(params: UpdateRoomAccountDataTask.Params) {
        return executeRequest(globalErrorReceiver) {
            roomApi.setRoomAccountData(userId, params.roomId, params.type, params.content)
        }
    }
}
