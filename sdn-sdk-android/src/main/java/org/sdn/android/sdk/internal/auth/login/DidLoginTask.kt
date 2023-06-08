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
import org.sdn.android.sdk.api.auth.LoginType
import org.sdn.android.sdk.api.auth.data.EdgeNodeConnectionConfig
import org.sdn.android.sdk.api.auth.data.LoginFlowTypes
import org.sdn.android.sdk.api.failure.Failure
import org.sdn.android.sdk.api.session.Session
import org.sdn.android.sdk.internal.auth.AuthAPI
import org.sdn.android.sdk.internal.auth.SessionCreator
import org.sdn.android.sdk.internal.auth.data.DidLoginParams
import org.sdn.android.sdk.internal.auth.data.Identifier
import org.sdn.android.sdk.internal.di.Unauthenticated
import org.sdn.android.sdk.internal.network.RetrofitFactory
import org.sdn.android.sdk.internal.network.executeRequest
import org.sdn.android.sdk.internal.network.httpclient.addSocketFactory
import org.sdn.android.sdk.internal.network.ssl.UnrecognizedCertificateException
import org.sdn.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface DidLoginTask : Task<DidLoginTask.Params, Session> {
    data class Params(
        val edgeNodeConnectionConfig: EdgeNodeConnectionConfig,
        val did: String,
        val nonce: String,
        val updated: String,
        val token: String,
        val deviceName: String,
        val deviceId: String?
    )
}

internal class DefaultDidLoginTask @Inject constructor(
        @Unauthenticated
        private val okHttpClient: Lazy<OkHttpClient>,
        private val retrofitFactory: RetrofitFactory,
        private val sessionCreator: SessionCreator
) : DidLoginTask {

    override suspend fun execute(params: DidLoginTask.Params): Session {
        val client = buildClient(params.edgeNodeConnectionConfig)
        val homeServerUrl = params.edgeNodeConnectionConfig.homeServerUriBase.toString()

        val authAPI = retrofitFactory.create(client, homeServerUrl)
                .create(AuthAPI::class.java)

        val loginParams = DidLoginParams(
            type = LoginFlowTypes.DID,
            updated = params.updated,
            randomServer = params.nonce,
            identifier = Identifier(
                did = params.did,
                token = params.token,
            )
        )

        val credentials = try {
            executeRequest(null) {
                authAPI.login(loginParams)
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

        return sessionCreator.createSession(credentials, params.edgeNodeConnectionConfig, LoginType.DIRECT)
    }

    private fun buildClient(edgeNodeConnectionConfig: EdgeNodeConnectionConfig): OkHttpClient {
        return okHttpClient.get()
                .newBuilder()
                .addSocketFactory(edgeNodeConnectionConfig)
                .build()
    }
}
