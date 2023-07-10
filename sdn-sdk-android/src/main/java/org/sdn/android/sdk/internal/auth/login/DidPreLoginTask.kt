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

package org.sdn.android.sdk.internal.auth.login

import dagger.Lazy
import okhttp3.OkHttpClient
import org.sdn.android.sdk.api.auth.data.EdgeNodeConnectionConfig
import org.sdn.android.sdk.api.auth.data.LoginDidMsg
import org.sdn.android.sdk.api.failure.Failure
import org.sdn.android.sdk.internal.auth.AuthAPI
import org.sdn.android.sdk.internal.auth.data.PreLoginParams
import org.sdn.android.sdk.internal.di.Unauthenticated
import org.sdn.android.sdk.internal.network.RetrofitFactory
import org.sdn.android.sdk.internal.network.executeRequest
import org.sdn.android.sdk.internal.network.httpclient.addSocketFactory
import org.sdn.android.sdk.internal.network.ssl.UnrecognizedCertificateException
import org.sdn.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface DidPreLoginTask : Task<DidPreLoginTask.Params, LoginDidMsg> {
    data class Params(
        val edgeNodeConnectionConfig: EdgeNodeConnectionConfig,
        val address: String
    )
}

internal class DefaultDidPreLoginTask @Inject constructor(
        @Unauthenticated
        private val okHttpClient: Lazy<OkHttpClient>,
        private val retrofitFactory: RetrofitFactory,
) : DidPreLoginTask {

    override suspend fun execute(params: DidPreLoginTask.Params): LoginDidMsg {
        val client = buildClient(params.edgeNodeConnectionConfig)
        val homeServerUrl = params.edgeNodeConnectionConfig.homeServerUriBase.toString()

        val authAPI = retrofitFactory.create(client, homeServerUrl)
                .create(AuthAPI::class.java)

        val loginDidMsg = try {

            val didListResp = executeRequest(null) {
                authAPI.didList(params.address)
            }
            val loginParams = if (didListResp.data.isNotEmpty()) {
                PreLoginParams("", didListResp.data[0])
            } else {
                PreLoginParams(params.address, "")
            }

            executeRequest(null) {
                authAPI.preLogin(loginParams)
            }
        } catch (throwable: Throwable) {
            throw when (throwable) {
                is UnrecognizedCertificateException -> Failure.UnrecognizedCertificateFailure(
                        homeServerUrl,
                        throwable.fingerprint
                )
                else -> throwable
            }
        }

        return loginDidMsg
    }

    private fun buildClient(edgeNodeConnectionConfig: EdgeNodeConnectionConfig): OkHttpClient {
        return okHttpClient.get()
                .newBuilder()
                .addSocketFactory(edgeNodeConnectionConfig)
                .build()
    }
}
