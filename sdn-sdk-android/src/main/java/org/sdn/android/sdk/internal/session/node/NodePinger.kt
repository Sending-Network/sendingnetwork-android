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

package org.sdn.android.sdk.internal.session.node

import kotlinx.coroutines.launch
import org.sdn.android.sdk.api.failure.Failure
import org.sdn.android.sdk.internal.network.executeRequest
import org.sdn.android.sdk.internal.task.TaskExecutor
import javax.inject.Inject

internal class NodePinger @Inject constructor(
        private val taskExecutor: TaskExecutor,
        private val capabilitiesAPI: CapabilitiesAPI
) {

    fun canReachHomeServer(callback: (Boolean) -> Unit) {
        taskExecutor.executorScope.launch {
            val canReach = canReachHomeServer()
            callback(canReach)
        }
    }

    suspend fun canReachHomeServer(): Boolean {
        return try {
            executeRequest(null) {
                capabilitiesAPI.ping()
            }
            true
        } catch (throwable: Throwable) {
            if (throwable is Failure.OtherServerError) {
                (throwable.httpCode == 404 || throwable.httpCode == 400)
            } else {
                false
            }
        }
    }
}
