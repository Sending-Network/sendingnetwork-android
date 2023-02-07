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

package org.sdn.android.sdk.internal.auth.db

import com.squareup.moshi.Moshi
import org.sdn.android.sdk.api.auth.data.EdgeNodeConnectionConfig
import org.sdn.android.sdk.internal.auth.login.ResetPasswordData
import org.sdn.android.sdk.internal.auth.registration.ThreePidData
import javax.inject.Inject

internal class PendingSessionMapper @Inject constructor(moshi: Moshi) {

    private val edgeNodeConnectionConfigAdapter = moshi.adapter(EdgeNodeConnectionConfig::class.java)
    private val resetPasswordDataAdapter = moshi.adapter(ResetPasswordData::class.java)
    private val threePidDataAdapter = moshi.adapter(ThreePidData::class.java)

    fun map(entity: PendingSessionEntity?): PendingSessionData? {
        if (entity == null) {
            return null
        }

        val homeServerConnectionConfig = edgeNodeConnectionConfigAdapter.fromJson(entity.homeServerConnectionConfigJson)!!
        val resetPasswordData = entity.resetPasswordDataJson?.let { resetPasswordDataAdapter.fromJson(it) }
        val threePidData = entity.currentThreePidDataJson?.let { threePidDataAdapter.fromJson(it) }

        return PendingSessionData(
                edgeNodeConnectionConfig = homeServerConnectionConfig,
                clientSecret = entity.clientSecret,
                sendAttempt = entity.sendAttempt,
                resetPasswordData = resetPasswordData,
                currentSession = entity.currentSession,
                isRegistrationStarted = entity.isRegistrationStarted,
                currentThreePidData = threePidData
        )
    }

    fun map(sessionData: PendingSessionData?): PendingSessionEntity? {
        if (sessionData == null) {
            return null
        }

        val homeServerConnectionConfigJson = edgeNodeConnectionConfigAdapter.toJson(sessionData.edgeNodeConnectionConfig)
        val resetPasswordDataJson = resetPasswordDataAdapter.toJson(sessionData.resetPasswordData)
        val currentThreePidDataJson = threePidDataAdapter.toJson(sessionData.currentThreePidData)

        return PendingSessionEntity(
                homeServerConnectionConfigJson = homeServerConnectionConfigJson,
                clientSecret = sessionData.clientSecret,
                sendAttempt = sessionData.sendAttempt,
                resetPasswordDataJson = resetPasswordDataJson,
                currentSession = sessionData.currentSession,
                isRegistrationStarted = sessionData.isRegistrationStarted,
                currentThreePidDataJson = currentThreePidDataJson
        )
    }
}
