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

package org.sdn.android.sdk.internal.session.room.delete

import org.sdn.android.sdk.api.query.QueryStringValue
import org.sdn.android.sdk.api.session.events.model.EventType
import org.sdn.android.sdk.api.session.events.model.toModel
import org.sdn.android.sdk.api.session.room.members.ChangeMembershipState
import org.sdn.android.sdk.api.session.room.model.create.RoomCreateContent
import org.sdn.android.sdk.internal.network.GlobalErrorReceiver
import org.sdn.android.sdk.internal.network.executeRequest
import org.sdn.android.sdk.internal.session.room.RoomAPI
import org.sdn.android.sdk.internal.session.room.membership.RoomChangeMembershipStateDataSource
import org.sdn.android.sdk.internal.session.room.state.StateEventDataSource
import org.sdn.android.sdk.internal.session.room.summary.RoomSummaryDataSource
import org.sdn.android.sdk.internal.task.Task
import timber.log.Timber
import javax.inject.Inject

internal interface DeleteRoomTask : Task<DeleteRoomTask.Params, Unit> {
    data class Params(
            val roomId: String,
            val reason: String?
    )
}

internal class DefaultDeleteRoomTask @Inject constructor(
        private val roomAPI: RoomAPI,
        private val globalErrorReceiver: GlobalErrorReceiver,
) : DeleteRoomTask {

    override suspend fun execute(params: DeleteRoomTask.Params) {
        executeRequest(globalErrorReceiver) {
            roomAPI.delete(params.roomId, mapOf("reason" to params.reason))
        }
    }
}
