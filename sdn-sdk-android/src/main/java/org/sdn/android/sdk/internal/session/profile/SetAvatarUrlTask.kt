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

package org.sdn.android.sdk.internal.session.profile

import org.sdn.android.sdk.internal.network.GlobalErrorReceiver
import org.sdn.android.sdk.internal.network.executeRequest
import org.sdn.android.sdk.internal.task.Task
import javax.inject.Inject

internal abstract class SetAvatarUrlTask : Task<SetAvatarUrlTask.Params, Unit> {
    data class Params(
            val userId: String,
            val newAvatarUrl: String
    )
}

internal class DefaultSetAvatarUrlTask @Inject constructor(
        private val profileAPI: ProfileAPI,
        private val globalErrorReceiver: GlobalErrorReceiver
) : SetAvatarUrlTask() {

    override suspend fun execute(params: Params) {
        val body = SetAvatarUrlBody(
                avatarUrl = params.newAvatarUrl
        )
        return executeRequest(globalErrorReceiver) {
            profileAPI.setAvatarUrl(params.userId, body)
        }
    }
}
