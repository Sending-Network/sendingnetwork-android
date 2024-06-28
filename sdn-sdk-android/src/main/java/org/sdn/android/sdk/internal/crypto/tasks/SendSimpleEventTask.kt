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
package org.sdn.android.sdk.internal.crypto.tasks

import org.sdn.android.sdk.api.session.events.model.Content
import org.sdn.android.sdk.api.session.events.model.LocalEcho
import org.sdn.android.sdk.internal.network.GlobalErrorReceiver
import org.sdn.android.sdk.internal.network.executeRequest
import org.sdn.android.sdk.internal.session.room.RoomAPI
import org.sdn.android.sdk.internal.task.Task
import timber.log.Timber
import javax.inject.Inject

internal interface SendSimpleEventTask : Task<SendSimpleEventTask.Params, String> {
    data class Params(
        val roomId: String,
        val eventType: String,
        val content: Content
    )
}

internal class DefaultSendSimpleEventTask @Inject constructor(
        private val roomAPI: RoomAPI,
        private val globalErrorReceiver: GlobalErrorReceiver
) : SendSimpleEventTask {

    override suspend fun execute(params: SendSimpleEventTask.Params): String {
        try {
            val localId = LocalEcho.createLocalEchoId()
            val response = executeRequest(globalErrorReceiver) {
                roomAPI.send(
                        localId,
                        roomId = params.roomId,
                        content = params.content,
                        eventType = params.eventType,
                )
            }
            return response.eventId.also {
                Timber.d("Event: $it just sent in ${params.roomId}")
            }
        } catch (e: Throwable) {
            Timber.w(e, "Unable to send the Event")
            throw e
        }
    }
}
