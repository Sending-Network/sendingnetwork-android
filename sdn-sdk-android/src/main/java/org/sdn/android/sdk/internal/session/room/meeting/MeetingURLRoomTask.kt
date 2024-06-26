/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.sdn.android.sdk.internal.session.room.meeting

import org.sdn.android.sdk.internal.crypto.keysbackup.api.RoomKeysApi
import org.sdn.android.sdk.internal.network.GlobalErrorReceiver
import org.sdn.android.sdk.internal.network.executeRequest
import org.sdn.android.sdk.internal.task.Task
import javax.inject.Inject
import  org.sdn.android.sdk.api.session.meetingResult.GetMeetingUrl

internal interface MeetingURLRoomTask : Task<Unit, GetMeetingUrl> {

}

internal class DefaultMeetingURLRoomTask @Inject constructor(
    private val roomKeysApiKey: RoomKeysApi,
    private val globalErrorReceiver: GlobalErrorReceiver

) : MeetingURLRoomTask {

    override suspend fun execute(params: Unit): GetMeetingUrl {
        println("executeRequest")
        return executeRequest(globalErrorReceiver){
            println("executeRequest")
            roomKeysApiKey.getMeetingUrl()
        }
    }
}
