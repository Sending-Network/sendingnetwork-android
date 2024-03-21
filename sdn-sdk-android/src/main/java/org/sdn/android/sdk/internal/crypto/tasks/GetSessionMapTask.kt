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

package org.sdn.android.sdk.internal.crypto.tasks

import org.sdn.android.sdk.internal.crypto.api.CryptoApi
import org.sdn.android.sdk.internal.network.GlobalErrorReceiver
import org.sdn.android.sdk.internal.network.executeRequest
import org.sdn.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface GetSessionMapTask : Task<GetSessionMapTask.Params, Map<String, Map<String, Any>>> {
    data class Params(val roomId: String, val sessionId: String)
}

internal class DefaultGetSessionMapTask @Inject constructor(
        private val cryptoApi: CryptoApi,
        private val globalErrorReceiver: GlobalErrorReceiver
) : GetSessionMapTask {

    override suspend fun execute(params: GetSessionMapTask.Params): Map<String, Map<String, Any>> {
        return executeRequest(globalErrorReceiver) {
            cryptoApi.getSessionMap(params.roomId, params.sessionId)
        }
    }
}

internal interface PutSessionMapTask : Task<PutSessionMapTask.Params, Unit> {
    data class Params(val roomId: String, val sessionId: String, val sessionMap: Map<String, Map<String, Any>>)
}

internal class DefaultPutSessionMapTask @Inject constructor(
    private val cryptoApi: CryptoApi,
    private val globalErrorReceiver: GlobalErrorReceiver
) : PutSessionMapTask {

    override suspend fun execute(params: PutSessionMapTask.Params) {
        return executeRequest(globalErrorReceiver) {
            cryptoApi.putSessionMap(params.roomId, params.sessionId, params.sessionMap)
        }
    }
}
