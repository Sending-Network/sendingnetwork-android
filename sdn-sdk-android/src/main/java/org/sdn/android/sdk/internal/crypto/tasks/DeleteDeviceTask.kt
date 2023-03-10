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

import androidx.annotation.Size
import org.sdn.android.sdk.api.auth.UIABaseAuth
import org.sdn.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.sdn.android.sdk.api.session.uia.UiaResult
import org.sdn.android.sdk.internal.auth.registration.handleUIA
import org.sdn.android.sdk.internal.crypto.api.CryptoApi
import org.sdn.android.sdk.internal.crypto.model.rest.DeleteDeviceParams
import org.sdn.android.sdk.internal.crypto.model.rest.DeleteDevicesParams
import org.sdn.android.sdk.internal.network.GlobalErrorReceiver
import org.sdn.android.sdk.internal.network.executeRequest
import org.sdn.android.sdk.internal.task.Task
import timber.log.Timber
import javax.inject.Inject

internal interface DeleteDeviceTask : Task<DeleteDeviceTask.Params, Unit> {
    data class Params(
            @Size(min = 1) val deviceIds: List<String>,
            val userInteractiveAuthInterceptor: UserInteractiveAuthInterceptor?,
            val userAuthParam: UIABaseAuth?
    )
}

internal class DefaultDeleteDeviceTask @Inject constructor(
        private val cryptoApi: CryptoApi,
        private val globalErrorReceiver: GlobalErrorReceiver
) : DeleteDeviceTask {

    override suspend fun execute(params: DeleteDeviceTask.Params) {
        require(params.deviceIds.isNotEmpty())

        try {
            executeRequest(globalErrorReceiver) {
                val userAuthParam = params.userAuthParam?.asMap()
                if (params.deviceIds.size == 1) {
                    cryptoApi.deleteDevice(
                            deviceId = params.deviceIds.first(),
                            DeleteDeviceParams(auth = userAuthParam)
                    )
                } else {
                    cryptoApi.deleteDevices(
                            DeleteDevicesParams(
                                    auth = userAuthParam,
                                    deviceIds = params.deviceIds
                            )
                    )
                }
            }
        } catch (throwable: Throwable) {
            if (params.userInteractiveAuthInterceptor == null ||
                    handleUIA(
                            failure = throwable,
                            interceptor = params.userInteractiveAuthInterceptor,
                            retryBlock = { authUpdate ->
                                execute(params.copy(userAuthParam = authUpdate))
                            }
                    ) != UiaResult.SUCCESS
            ) {
                Timber.d("## UIA: propagate failure")
                throw throwable
            }
        }
    }
}
