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
import org.sdn.android.sdk.api.auth.data.*
import org.sdn.android.sdk.api.failure.Failure
import org.sdn.android.sdk.internal.auth.AuthAPI
import org.sdn.android.sdk.internal.auth.data.DidSaveParams
import org.sdn.android.sdk.internal.di.Unauthenticated
import org.sdn.android.sdk.internal.network.RetrofitFactory
import org.sdn.android.sdk.internal.network.executeRequest
import org.sdn.android.sdk.internal.network.httpclient.addSocketFactory
import org.sdn.android.sdk.internal.network.ssl.UnrecognizedCertificateException
import org.sdn.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface DidSaveTask : Task<DidSaveTask.Params, DidSaveResp> {
    data class Params(
        val edgeNodeConnectionConfig: EdgeNodeConnectionConfig,
        val did: String,
        val signature: String,
        val operation: String,
        val ids: List<String>?,
        val address: String?,
        val updated: String,
    )
}

internal class DefaultDidSaveTask @Inject constructor(
        @Unauthenticated
        private val okHttpClient: Lazy<OkHttpClient>,
        private val retrofitFactory: RetrofitFactory,
) : DidSaveTask {

    override suspend fun execute(params: DidSaveTask.Params): DidSaveResp {
        val client = buildClient(params.edgeNodeConnectionConfig)
        val homeServerUrl = params.edgeNodeConnectionConfig.homeServerUriBase.toString()

        val authAPI = retrofitFactory.create(client, homeServerUrl)
                .create(AuthAPI::class.java)

        val didSaveResp = try {
            executeRequest(null) {
                authAPI.didSave(
                    params.did,
                    DidSaveParams(
                        params.signature,
                        params.operation,
                        params.ids,
                        params.address,
                        params.updated,
                    )
                )
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

        return didSaveResp
    }

    private fun buildClient(edgeNodeConnectionConfig: EdgeNodeConnectionConfig): OkHttpClient {
        return okHttpClient.get()
                .newBuilder()
                .addSocketFactory(edgeNodeConnectionConfig)
                .build()
    }
}
