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
import org.sdn.android.sdk.internal.crypto.model.rest.PullKeysBody
import org.sdn.android.sdk.internal.network.GlobalErrorReceiver
import org.sdn.android.sdk.internal.network.executeRequest
import org.sdn.android.sdk.internal.task.Task
import org.sdn.android.sdk.api.session.sync.model.ToDeviceSyncResponse
import timber.log.Timber
import javax.inject.Inject

internal interface PullSessionKeysTask : Task<PullSessionKeysTask.Params, ToDeviceSyncResponse> {
    data class Params(
        val sessionId: String
    )
}

internal class DefaultPullSessionKeysTask @Inject constructor(
        private val cryptoApi: CryptoApi,
        private val globalErrorReceiver: GlobalErrorReceiver
) : PullSessionKeysTask {

    override suspend fun execute(params: PullSessionKeysTask.Params): ToDeviceSyncResponse {
        val body = PullKeysBody (
                traceId = params.sessionId
        )

        Timber.i("## pulling session keys for -> ${body.traceId}")

        return executeRequest(globalErrorReceiver) {
            cryptoApi.pullRoomKeys(body)
        }
    }
}
