/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.sdn.android.sdk.internal.auth.registration

import org.sdn.android.sdk.api.auth.data.Credentials
import org.sdn.android.sdk.api.failure.Failure
import org.sdn.android.sdk.api.failure.toRegistrationFlowResponse
import org.sdn.android.sdk.internal.auth.AuthAPI
import org.sdn.android.sdk.internal.network.executeRequest
import org.sdn.android.sdk.internal.task.Task

internal interface RegisterCustomTask : Task<RegisterCustomTask.Params, Credentials> {
    data class Params(
            val registrationCustomParams: RegistrationCustomParams
    )
}

internal class DefaultRegisterCustomTask(
        private val authAPI: AuthAPI
) : RegisterCustomTask {

    override suspend fun execute(params: RegisterCustomTask.Params): Credentials {
        try {
            return executeRequest(null) {
                authAPI.registerCustom(params.registrationCustomParams)
            }
        } catch (throwable: Throwable) {
            throw throwable.toRegistrationFlowResponse()
                    ?.let { Failure.RegistrationFlowError(it) }
                    ?: throwable
        }
    }
}
