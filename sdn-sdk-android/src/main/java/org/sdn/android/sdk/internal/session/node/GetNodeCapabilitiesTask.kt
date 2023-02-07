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

import com.zhuinden.monarchy.Monarchy
import org.sdn.android.sdk.api.MatrixPatterns.getServerName
import org.sdn.android.sdk.api.auth.data.EdgeNodeConnectionConfig
import org.sdn.android.sdk.api.auth.wellknown.WellknownResult
import org.sdn.android.sdk.api.extensions.orTrue
import org.sdn.android.sdk.api.session.node.NodeCapabilities
import org.sdn.android.sdk.internal.auth.version.Versions
import org.sdn.android.sdk.internal.auth.version.doesServerSupportLogoutDevices
import org.sdn.android.sdk.internal.auth.version.doesServerSupportQrCodeLogin
import org.sdn.android.sdk.internal.auth.version.doesServerSupportRemoteToggleOfPushNotifications
import org.sdn.android.sdk.internal.auth.version.doesServerSupportThreadUnreadNotifications
import org.sdn.android.sdk.internal.auth.version.doesServerSupportThreads
import org.sdn.android.sdk.internal.auth.version.isLoginAndRegistrationSupportedBySdk
import org.sdn.android.sdk.internal.database.model.HomeServerCapabilitiesEntity
import org.sdn.android.sdk.internal.database.query.getOrCreate
import org.sdn.android.sdk.internal.di.MoshiProvider
import org.sdn.android.sdk.internal.di.SessionDatabase
import org.sdn.android.sdk.internal.di.UserId
import org.sdn.android.sdk.internal.network.GlobalErrorReceiver
import org.sdn.android.sdk.internal.network.executeRequest
import org.sdn.android.sdk.internal.session.integrationmanager.IntegrationManagerConfigExtractor
import org.sdn.android.sdk.internal.session.media.GetMediaConfigResult
import org.sdn.android.sdk.internal.session.media.MediaAPI
import org.sdn.android.sdk.internal.task.Task
import org.sdn.android.sdk.internal.util.awaitTransaction
import org.sdn.android.sdk.internal.wellknown.GetWellknownTask
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

internal interface GetNodeCapabilitiesTask : Task<GetNodeCapabilitiesTask.Params, Unit> {
    data class Params(
            val forceRefresh: Boolean
    )
}

internal class DefaultGetNodeCapabilitiesTask @Inject constructor(
    private val capabilitiesAPI: CapabilitiesAPI,
    private val mediaAPI: MediaAPI,
    @SessionDatabase private val monarchy: Monarchy,
    private val globalErrorReceiver: GlobalErrorReceiver,
    private val getWellknownTask: GetWellknownTask,
    private val configExtractor: IntegrationManagerConfigExtractor,
    private val edgeNodeConnectionConfig: EdgeNodeConnectionConfig,
    @UserId
        private val userId: String
) : GetNodeCapabilitiesTask {

    override suspend fun execute(params: GetNodeCapabilitiesTask.Params) {
        var doRequest = params.forceRefresh
        if (!doRequest) {
            monarchy.awaitTransaction { realm ->
                val homeserverCapabilitiesEntity = HomeServerCapabilitiesEntity.getOrCreate(realm)

                doRequest = homeserverCapabilitiesEntity.lastUpdatedTimestamp + MIN_DELAY_BETWEEN_TWO_REQUEST_MILLIS < Date().time
            }
        }

        if (!doRequest) {
            return
        }

        val capabilities = runCatching {
            executeRequest(globalErrorReceiver) {
                capabilitiesAPI.getCapabilities()
            }
        }.getOrNull()

        val mediaConfig = runCatching {
            executeRequest(globalErrorReceiver) {
                mediaAPI.getMediaConfig()
            }
        }.getOrNull()

        val versions = runCatching {
            executeRequest(null) {
                capabilitiesAPI.getVersions()
            }
        }.getOrNull()

        // Domain may include a port (eg, matrix.org:8080)
        // Per https://spec.matrix.org/latest/client-server-api/#well-known-uri we should extract the hostname from the server name
        // So we take everything before the last : as the domain for the well-known task.
        // NB: This is not always the same endpoint as capabilities / mediaConfig uses.
        val wellknownResult = runCatching {
            getWellknownTask.execute(
                    GetWellknownTask.Params(
                            domain = userId.getServerName().substringBeforeLast(":"),
                            edgeNodeConnectionConfig = edgeNodeConnectionConfig
                    )
            )
        }.getOrNull()

        insertInDb(capabilities, mediaConfig, versions, wellknownResult)
    }

    private suspend fun insertInDb(
            getCapabilitiesResult: GetCapabilitiesResult?,
            getMediaConfigResult: GetMediaConfigResult?,
            getVersionResult: Versions?,
            getWellknownResult: WellknownResult?
    ) {
        monarchy.awaitTransaction { realm ->
            val homeserverCapabilitiesEntity = HomeServerCapabilitiesEntity.getOrCreate(realm)

            if (getCapabilitiesResult != null) {
                val capabilities = getCapabilitiesResult.capabilities

                // The spec says: If not present, the client should assume that
                // password, display name, avatar changes and 3pid changes are possible via the API
                homeserverCapabilitiesEntity.canChangePassword = capabilities?.changePassword?.enabled.orTrue()
                homeserverCapabilitiesEntity.canChangeDisplayName = capabilities?.changeDisplayName?.enabled.orTrue()
                homeserverCapabilitiesEntity.canChangeAvatar = capabilities?.changeAvatar?.enabled.orTrue()
                homeserverCapabilitiesEntity.canChange3pid = capabilities?.change3pid?.enabled.orTrue()

                homeserverCapabilitiesEntity.roomVersionsJson = capabilities?.roomVersions?.let {
                    MoshiProvider.providesMoshi().adapter(RoomVersions::class.java).toJson(it)
                }
            }

            if (getMediaConfigResult != null) {
                homeserverCapabilitiesEntity.maxUploadFileSize = getMediaConfigResult.maxUploadSize
                        ?: NodeCapabilities.MAX_UPLOAD_FILE_SIZE_UNKNOWN
            }

            if (getVersionResult != null) {
                homeserverCapabilitiesEntity.lastVersionIdentityServerSupported =
                        getVersionResult.isLoginAndRegistrationSupportedBySdk()
                homeserverCapabilitiesEntity.canControlLogoutDevices =
                        getVersionResult.doesServerSupportLogoutDevices()
                homeserverCapabilitiesEntity.canUseThreading = /* capabilities?.threads?.enabled.orFalse() || */
                        getVersionResult.doesServerSupportThreads()
                homeserverCapabilitiesEntity.canUseThreadReadReceiptsAndNotifications =
                        getVersionResult.doesServerSupportThreadUnreadNotifications()
                homeserverCapabilitiesEntity.canLoginWithQrCode =
                        getVersionResult.doesServerSupportQrCodeLogin()
                homeserverCapabilitiesEntity.canRemotelyTogglePushNotificationsOfDevices =
                        getVersionResult.doesServerSupportRemoteToggleOfPushNotifications()
            }

            if (getWellknownResult != null && getWellknownResult is WellknownResult.Prompt) {
                homeserverCapabilitiesEntity.defaultIdentityServerUrl = getWellknownResult.identityServerUrl
                // We are also checking for integration manager configurations
                val config = configExtractor.extract(getWellknownResult.wellKnown)
                if (config != null) {
                    Timber.v("Extracted integration config : $config")
                    realm.insertOrUpdate(config)
                }
            }
            homeserverCapabilitiesEntity.lastUpdatedTimestamp = Date().time
        }
    }

    companion object {
        // 8 hours like on Element Web
        private const val MIN_DELAY_BETWEEN_TWO_REQUEST_MILLIS = 8 * 60 * 60 * 1000
    }
}
