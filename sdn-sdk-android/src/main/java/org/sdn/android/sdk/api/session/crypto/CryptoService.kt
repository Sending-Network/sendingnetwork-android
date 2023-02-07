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

package org.sdn.android.sdk.api.session.crypto

import android.content.Context
import androidx.annotation.Size
import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import org.sdn.android.sdk.api.SDNCallback
import org.sdn.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.sdn.android.sdk.api.listeners.ProgressListener
import org.sdn.android.sdk.api.session.crypto.crosssigning.CrossSigningService
import org.sdn.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.sdn.android.sdk.api.session.crypto.keysbackup.KeysBackupService
import org.sdn.android.sdk.api.session.crypto.keyshare.GossipingRequestListener
import org.sdn.android.sdk.api.session.crypto.model.AuditTrail
import org.sdn.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.sdn.android.sdk.api.session.crypto.model.DeviceInfo
import org.sdn.android.sdk.api.session.crypto.model.DevicesListResponse
import org.sdn.android.sdk.api.session.crypto.model.ImportRoomKeysResult
import org.sdn.android.sdk.api.session.crypto.model.IncomingRoomKeyRequest
import org.sdn.android.sdk.api.session.crypto.model.MXDeviceInfo
import org.sdn.android.sdk.api.session.crypto.model.MXEncryptEventContentResult
import org.sdn.android.sdk.api.session.crypto.model.MXEventDecryptionResult
import org.sdn.android.sdk.api.session.crypto.model.MXUsersDevicesMap
import org.sdn.android.sdk.api.session.crypto.verification.VerificationService
import org.sdn.android.sdk.api.session.events.model.Content
import org.sdn.android.sdk.api.session.events.model.Event
import org.sdn.android.sdk.api.session.events.model.content.RoomKeyWithHeldContent
import org.sdn.android.sdk.api.util.Optional
import org.sdn.android.sdk.internal.crypto.model.SessionInfo

interface CryptoService {

    fun verificationService(): VerificationService

    fun crossSigningService(): CrossSigningService

    fun keysBackupService(): KeysBackupService

    fun setDeviceName(deviceId: String, deviceName: String, callback: SDNCallback<Unit>)

    fun deleteDevice(deviceId: String, userInteractiveAuthInterceptor: UserInteractiveAuthInterceptor, callback: SDNCallback<Unit>)

    fun deleteDevices(@Size(min = 1) deviceIds: List<String>, userInteractiveAuthInterceptor: UserInteractiveAuthInterceptor, callback: SDNCallback<Unit>)

    fun getCryptoVersion(context: Context, longFormat: Boolean): String

    fun isCryptoEnabled(): Boolean

    fun isRoomBlacklistUnverifiedDevices(roomId: String?): Boolean

    fun getLiveBlockUnverifiedDevices(roomId: String): LiveData<Boolean>

    fun setWarnOnUnknownDevices(warn: Boolean)

    fun setDeviceVerification(trustLevel: DeviceTrustLevel, userId: String, deviceId: String)

    fun getUserDevices(userId: String): MutableList<CryptoDeviceInfo>

    fun setDevicesKnown(devices: List<MXDeviceInfo>, callback: SDNCallback<Unit>?)

    fun deviceWithIdentityKey(senderKey: String, algorithm: String): CryptoDeviceInfo?

    fun getMyDevice(): CryptoDeviceInfo

    fun getGlobalBlacklistUnverifiedDevices(): Boolean

    fun setGlobalBlacklistUnverifiedDevices(block: Boolean)

    fun getLiveGlobalCryptoConfig(): LiveData<GlobalCryptoConfig>

    /**
     * Enable or disable key gossiping.
     * Default is true.
     * If set to false this device won't send key_request nor will accept key forwarded
     */
    fun enableKeyGossiping(enable: Boolean)

    fun isKeyGossipingEnabled(): Boolean

    /**
     * As per MSC3061.
     * If true will make it possible to share part of e2ee room history
     * on invite depending on the room visibility setting.
     */
    fun enableShareKeyOnInvite(enable: Boolean)

    /**
     * As per MSC3061.
     * If true will make it possible to share part of e2ee room history
     * on invite depending on the room visibility setting.
     */
    fun isShareKeysOnInviteEnabled(): Boolean

    fun setRoomUnBlockUnverifiedDevices(roomId: String)

    fun getDeviceTrackingStatus(userId: String): Int

    suspend fun importRoomKeys(
            roomKeysAsArray: ByteArray,
            password: String,
            progressListener: ProgressListener?
    ): ImportRoomKeysResult

    suspend fun exportRoomKeys(password: String): ByteArray

    fun setRoomBlockUnverifiedDevices(roomId: String, block: Boolean)

    fun getCryptoDeviceInfo(userId: String, deviceId: String?): CryptoDeviceInfo?

    fun getCryptoDeviceInfo(deviceId: String, callback: SDNCallback<DeviceInfo>)

    fun getCryptoDeviceInfo(userId: String): List<CryptoDeviceInfo>

    fun getLiveCryptoDeviceInfo(): LiveData<List<CryptoDeviceInfo>>

    fun getLiveCryptoDeviceInfoWithId(deviceId: String): LiveData<Optional<CryptoDeviceInfo>>

    fun getLiveCryptoDeviceInfo(userId: String): LiveData<List<CryptoDeviceInfo>>

    fun getLiveCryptoDeviceInfo(userIds: List<String>): LiveData<List<CryptoDeviceInfo>>

    fun requestRoomKeyForEvent(event: Event)

    fun reRequestRoomKeyForEvent(event: Event)

    fun addRoomKeysRequestListener(listener: GossipingRequestListener)

    fun removeRoomKeysRequestListener(listener: GossipingRequestListener)

    fun fetchDevicesList(callback: SDNCallback<DevicesListResponse>)

    fun getMyDevicesInfo(): List<DeviceInfo>

    fun getMyDevicesInfoLive(): LiveData<List<DeviceInfo>>

    fun getMyDevicesInfoLive(deviceId: String): LiveData<Optional<DeviceInfo>>

    fun inboundGroupSessionsCount(onlyBackedUp: Boolean): Int

    fun isRoomEncrypted(roomId: String): Boolean

    // TODO This could be removed from this interface
    fun encryptEventContent(
            eventContent: Content,
            eventType: String,
            roomId: String,
            callback: SDNCallback<MXEncryptEventContentResult>
    )

    fun discardOutboundSession(roomId: String)

    @Throws(MXCryptoError::class)
    suspend fun decryptEvent(event: Event, timeline: String): MXEventDecryptionResult

    fun decryptEventAsync(event: Event, timeline: String, callback: SDNCallback<MXEventDecryptionResult>)

    fun getEncryptionAlgorithm(roomId: String): String?

    fun shouldEncryptForInvitedMembers(roomId: String): Boolean

    fun downloadKeys(userIds: List<String>, forceDownload: Boolean, callback: SDNCallback<MXUsersDevicesMap<CryptoDeviceInfo>>)

    fun addNewSessionListener(newSessionListener: NewSessionListener)
    fun removeSessionListener(listener: NewSessionListener)

    fun getOutgoingRoomKeyRequests(): List<OutgoingKeyRequest>
    fun getOutgoingRoomKeyRequestsPaged(): LiveData<PagedList<OutgoingKeyRequest>>

    fun getIncomingRoomKeyRequests(): List<IncomingRoomKeyRequest>
    fun getIncomingRoomKeyRequestsPaged(): LiveData<PagedList<IncomingRoomKeyRequest>>

    /**
     * Can be called by the app layer to accept a request manually.
     * Use carefully as it is prone to social attacks.
     */
    suspend fun manuallyAcceptRoomKeyRequest(request: IncomingRoomKeyRequest)

    fun getGossipingEventsTrail(): LiveData<PagedList<AuditTrail>>
    fun getGossipingEvents(): List<AuditTrail>

    // For testing shared session
    fun getSharedWithInfo(roomId: String?, sessionId: String): MXUsersDevicesMap<Int>
    fun getWithHeldMegolmSession(roomId: String, sessionId: String): RoomKeyWithHeldContent?

    /**
     * Perform any background tasks that can be done before a message is ready to
     * send, in order to speed up sending of the message.
     */
    fun prepareToEncrypt(roomId: String, callback: SDNCallback<Unit>)

    /**
     * Share all inbound sessions of the last chunk messages to the provided userId devices.
     */
    suspend fun sendSharedHistoryKeys(roomId: String, userId: String, sessionInfoSet: Set<SessionInfo>?)
}
