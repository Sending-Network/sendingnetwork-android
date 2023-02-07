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

import org.sdn.android.sdk.api.session.identity.IdentityServiceError
import org.sdn.android.sdk.api.session.identity.ThreePid
import org.sdn.android.sdk.internal.di.AuthenticatedIdentity
import org.sdn.android.sdk.internal.network.GlobalErrorReceiver
import org.sdn.android.sdk.internal.network.executeRequest
import org.sdn.android.sdk.internal.network.token.AccessTokenProvider
import org.sdn.android.sdk.internal.session.identity.data.IdentityStore
import org.sdn.android.sdk.internal.session.identity.data.getIdentityServerUrlWithoutProtocol
import org.sdn.android.sdk.internal.task.Task
import javax.inject.Inject

internal abstract class BindThreePidsTask : Task<BindThreePidsTask.Params, Unit> {
    data class Params(
            val threePid: ThreePid
    )
}

internal class DefaultBindThreePidsTask @Inject constructor(
        private val profileAPI: ProfileAPI,
        private val identityStore: IdentityStore,
        @AuthenticatedIdentity private val accessTokenProvider: AccessTokenProvider,
        private val globalErrorReceiver: GlobalErrorReceiver
) : BindThreePidsTask() {
    override suspend fun execute(params: Params) {
        val identityServerUrlWithoutProtocol = identityStore.getIdentityServerUrlWithoutProtocol() ?: throw IdentityServiceError.NoIdentityServerConfigured
        val identityServerAccessToken = accessTokenProvider.getToken() ?: throw IdentityServiceError.NoIdentityServerConfigured
        val identityPendingBinding = identityStore.getPendingBinding(params.threePid) ?: throw IdentityServiceError.NoCurrentBindingError

        executeRequest(globalErrorReceiver) {
            profileAPI.bindThreePid(
                    BindThreePidBody(
                            clientSecret = identityPendingBinding.clientSecret,
                            identityServerUrlWithoutProtocol = identityServerUrlWithoutProtocol,
                            identityServerAccessToken = identityServerAccessToken,
                            sid = identityPendingBinding.sid
                    )
            )
        }

        // Binding is over, cleanup the store
        identityStore.deletePendingBinding(params.threePid)
    }
}
