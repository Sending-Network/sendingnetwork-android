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

package org.sdn.android.sdk.internal.crypto.algorithms.megolm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.sdn.android.sdk.api.SDNCoroutineDispatchers
import org.sdn.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_RATCHET
import org.sdn.android.sdk.api.extensions.tryOrNull
import org.sdn.android.sdk.api.logger.LoggerTag
import org.sdn.android.sdk.api.session.crypto.MXCryptoError
import org.sdn.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.sdn.android.sdk.api.session.crypto.model.MXUsersDevicesMap
import org.sdn.android.sdk.api.session.crypto.model.forEach
import org.sdn.android.sdk.api.session.events.model.Content
import org.sdn.android.sdk.api.session.events.model.EventType
import org.sdn.android.sdk.api.session.events.model.content.RoomKeyWithHeldContent
import org.sdn.android.sdk.api.session.events.model.content.WithHeldCode
import org.sdn.android.sdk.api.session.room.model.message.MessageContent
import org.sdn.android.sdk.api.session.room.model.message.MessageType
import org.sdn.android.sdk.internal.crypto.DeviceListManager
import org.sdn.android.sdk.internal.crypto.InboundGroupSessionHolder
import org.sdn.android.sdk.internal.crypto.MXOlmDevice
import org.sdn.android.sdk.internal.crypto.MegolmSessionData
import org.sdn.android.sdk.internal.crypto.actions.EnsureOlmSessionsForDevicesAction
import org.sdn.android.sdk.internal.crypto.actions.MessageEncrypter
import org.sdn.android.sdk.internal.crypto.algorithms.IMXEncrypting
import org.sdn.android.sdk.internal.crypto.algorithms.IMXGroupEncryption
import org.sdn.android.sdk.internal.crypto.keysbackup.DefaultKeysBackupService
import org.sdn.android.sdk.internal.crypto.model.toDebugCount
import org.sdn.android.sdk.internal.crypto.model.toDebugString
import org.sdn.android.sdk.internal.crypto.repository.WarnOnUnknownDeviceRepository
import org.sdn.android.sdk.internal.crypto.store.IMXCryptoStore
import org.sdn.android.sdk.internal.crypto.tasks.GetSessionMapTask
import org.sdn.android.sdk.internal.crypto.tasks.PutSessionMapTask
import org.sdn.android.sdk.internal.crypto.tasks.SendToDeviceTask
import org.sdn.android.sdk.internal.util.JsonCanonicalizer
import org.sdn.android.sdk.internal.util.convertToUTF8
import org.sdn.android.sdk.internal.util.time.Clock
import timber.log.Timber
import java.lang.Exception

private val loggerTag = LoggerTag("MXRatchetEncryption", LoggerTag.CRYPTO)

internal class MXRatchetEncryption(
    // The id of the room we will be sending to.
    private val roomId: String,
    private val olmDevice: MXOlmDevice,
    private val defaultKeysBackupService: DefaultKeysBackupService,
    private val cryptoStore: IMXCryptoStore,
    private val deviceListManager: DeviceListManager,
    private val ensureOlmSessionsForDevicesAction: EnsureOlmSessionsForDevicesAction,
    private val myUserId: String,
    private val myDeviceId: String,
    private val sendToDeviceTask: SendToDeviceTask,
    private val getSessionMapTask: GetSessionMapTask,
    private val putSessionMapTask: PutSessionMapTask,
    private val messageEncrypter: MessageEncrypter,
    private val warnOnUnknownDevicesRepository: WarnOnUnknownDeviceRepository,
    private val coroutineDispatchers: SDNCoroutineDispatchers,
    private val cryptoCoroutineScope: CoroutineScope,
    private val clock: Clock,
) : IMXEncrypting, IMXGroupEncryption {

    // OutboundSessionInfo. Null if we haven't yet started setting one up. Note
    // that even if this is non-null, it may not be ready for use (in which
    // case outboundSession.shareOperation will be non-null.)
    private var outboundSession: MXOutboundSessionInfo? = null

    init {
        // restore existing outbound session if any
        outboundSession = olmDevice.restoreOutboundGroupSessionForRoom(roomId)
    }

    override suspend fun encryptEventContent(
        eventContent: Content,
        eventType: String,
        userIds: List<String>
    ): Content {
        val ts = clock.epochMillis()
        Timber.tag(loggerTag.value).v("encryptEventContent : getDevicesInRoom")

        /**
         * When using in-room messages and the room has encryption enabled,
         * clients should ensure that encryption does not hinder the verification.
         * For example, if the verification messages are encrypted, clients must ensure that all the recipient’s
         * unverified devices receive the keys necessary to decrypt the messages,
         * even if they would normally not be given the keys to decrypt messages in the room.
         */
        val shouldSendToUnverified = isVerificationEvent(eventType, eventContent)

        val devices = getDevicesInRoom(userIds, forceDistributeToUnverified = shouldSendToUnverified)

        Timber.tag(loggerTag.value).d("encrypt event in room=$roomId - devices count in room ${devices.allowedDevices.toDebugCount()}")
        Timber.tag(loggerTag.value).v("encryptEventContent ${clock.epochMillis() - ts}: getDevicesInRoom ${devices.allowedDevices.toDebugString()}")
        val outboundSession = ensureCurrentSession(devices.allowedDevices)

        return encryptContent(outboundSession, eventType, eventContent)
            .also {
                notifyWithheldForSession(devices.withHeldDevices, outboundSession)
                // annoyingly we have to serialize again the saved outbound session to store message index :/
                // if not we would see duplicate message index errors
                olmDevice.storeOutboundGroupSessionForRoom(roomId, outboundSession.sessionId)
                Timber.tag(loggerTag.value).d("encrypt event in room=$roomId Finished in ${clock.epochMillis() - ts} millis")
            }
    }

    private fun isVerificationEvent(eventType: String, eventContent: Content) =
        EventType.isVerificationEvent(eventType) ||
                (eventType == EventType.MESSAGE &&
                        eventContent.get(MessageContent.MSG_TYPE_JSON_KEY) == MessageType.MSGTYPE_VERIFICATION_REQUEST)

    private fun notifyWithheldForSession(devices: MXUsersDevicesMap<WithHeldCode>, outboundSession: MXOutboundSessionInfo) {
        // offload to computation thread
        cryptoCoroutineScope.launch(coroutineDispatchers.computation) {
            mutableListOf<Pair<UserDevice, WithHeldCode>>().apply {
                devices.forEach { userId, deviceId, withheldCode ->
                    this.add(UserDevice(userId, deviceId) to withheldCode)
                }
            }.groupBy(
                { it.second },
                { it.first }
            ).forEach { (code, targets) ->
                notifyKeyWithHeld(targets, outboundSession.sessionId, olmDevice.deviceCurve25519Key, code)
            }
        }
    }

    override fun discardSessionKey() {
        outboundSession = null
        olmDevice.discardOutboundGroupSessionForRoom(roomId)
    }

    override suspend fun preshareKey(userIds: List<String>) {
        val ts = clock.epochMillis()
        Timber.tag(loggerTag.value).d("preshareKey started in $roomId ...")
        val devices = getDevicesInRoom(userIds)
        val outboundSession = ensureCurrentSession(devices.allowedDevices)

        notifyWithheldForSession(devices.withHeldDevices, outboundSession)

        Timber.tag(loggerTag.value).d("preshareKey in $roomId done in  ${clock.epochMillis() - ts} millis")
    }

    /**
     * Prepare a new session.
     *
     * @return the session description
     */
    private fun prepareNewSessionInRoom(): MXOutboundSessionInfo {
        Timber.tag(loggerTag.value).v("prepareNewSessionInRoom() ")
        val sessionId = olmDevice.createOutboundGroupSessionForRoom(roomId)

        val keysClaimedMap = mapOf(
            "ed25519" to olmDevice.deviceEd25519Key!!
        )

        val sharedHistory = cryptoStore.shouldShareHistory(roomId)
        Timber.tag(loggerTag.value).v("prepareNewSessionInRoom() as sharedHistory $sharedHistory")
        olmDevice.addInboundGroupSession(
            sessionId = sessionId!!,
            sessionKey = olmDevice.getSessionKey(sessionId)!!,
            roomId = roomId,
            algorithm = MXCRYPTO_ALGORITHM_RATCHET,
            senderKey = olmDevice.deviceCurve25519Key!!,
            forwardingCurve25519KeyChain = emptyList(),
            keysClaimed = keysClaimedMap,
            exportFormat = false,
            sharedHistory = sharedHistory,
            trusted = true
        )

        defaultKeysBackupService.maybeBackupKeys()

        return MXOutboundSessionInfo(
            sessionId = sessionId,
            senderKey = olmDevice.deviceCurve25519Key!!,
            sharedWithHelper = SharedWithHelper(roomId, sessionId, cryptoStore),
            clock = clock,
            sharedHistory = sharedHistory
        )
    }

    /**
     * Ensure the current session.
     *
     * @param devicesInRoom the devices list
     */
    private suspend fun ensureCurrentSession(devicesInRoom: MXUsersDevicesMap<CryptoDeviceInfo>): MXOutboundSessionInfo {
        Timber.tag(loggerTag.value).v("ensureCurrentSession roomId:$roomId")
        val sharedHistory = cryptoStore.shouldShareHistory(roomId)
        val safeSession: MXOutboundSessionInfo
        val exportedSession: MegolmSessionData?

        val outboundSession = this.outboundSession
        if (outboundSession != null) {
            Timber.tag(loggerTag.value).i("ensureCurrentSession() : reuse outbound session ${outboundSession.sessionId} in $roomId")
            safeSession = outboundSession
            val inboundGroupSessionHolder = olmDevice.getInboundGroupSession(outboundSession.sessionId, outboundSession.senderKey, roomId)
            exportedSession = inboundGroupSessionHolder.mutex.withLock {
                inboundGroupSessionHolder.wrapper.exportKeys()
            }
        } else {
            val currentGroupSession = olmDevice.getCurrentGroupSession(roomId)
            val sessionId = currentGroupSession?.wrapper?.safeSessionId
            if (currentGroupSession != null && sessionId != null) {
                Timber.tag(loggerTag.value).i("ensureCurrentSession() : reuse existing session $sessionId in $roomId")
                safeSession = MXOutboundSessionInfo(
                    sessionId = sessionId,
                    senderKey = currentGroupSession.wrapper.senderKey,
                    sharedWithHelper = SharedWithHelper(roomId, sessionId, cryptoStore),
                    clock = clock,
                    sharedHistory = sharedHistory
                )
                exportedSession = currentGroupSession.mutex.withLock {
                    currentGroupSession.wrapper.exportKeys()
                }
            } else {
                safeSession = prepareNewSessionInRoom()
                exportedSession = null
                Timber.tag(loggerTag.value).i("ensureCurrentSession() : use new session ${safeSession.sessionId} in $roomId")
            }
            this.outboundSession = safeSession
        }

        var remoteSessionMap: Map<String, Map<String, Any>>? = null
        if (safeSession.sharedSessionMap.isEmpty()) {
            val sharedWithDevices = safeSession.sharedWithHelper.sharedWithDevices()
            if (!sharedWithDevices.isEmpty) {
                safeSession.sharedSessionMap = sharedWithDevices.map
            } else {
                remoteSessionMap = getSessionMapTask.execute(GetSessionMapTask.Params(roomId, safeSession.sessionId))
                safeSession.sharedSessionMap = remoteSessionMap
            }
        }

        val shareMap = HashMap<String, MutableList<CryptoDeviceInfo>>()/* userId */
        val userIds = devicesInRoom.userIds
        var skipCount = 0
        for (userId in userIds) {
            val deviceIds = devicesInRoom.getUserDeviceIds(userId)
            for (deviceId in deviceIds!!) {
                val deviceInfo = devicesInRoom.getObject(userId, deviceId)
                if (deviceInfo != null && safeSession.sharedSessionMap[userId]?.get(deviceId) == null) {
                    val devices = shareMap.getOrPut(userId) { ArrayList() }
                    devices.add(deviceInfo)
                } else {
                    skipCount++
                }
            }
        }
        val devicesCount = shareMap.entries.fold(0) { acc, new -> acc + new.value.size }
        Timber.tag(loggerTag.value).d("roomId:$roomId found $devicesCount devices without megolm session(${safeSession.sessionId}), skip count: $skipCount")

        safeSession.updatedUserDevices.clear()
        // measure time consumed
        val ts = clock.epochMillis()
        shareKey(safeSession, exportedSession, shareMap)
        Timber.tag(loggerTag.value).d("shareKey in $roomId done in ${clock.epochMillis() - ts} millis")

        if (!remoteSessionMap.isNullOrEmpty()) {
            cryptoStore.batchMarkedSessionAsShared(roomId, safeSession.sessionId, remoteSessionMap)
            Timber.tag(loggerTag.value).d("batchMarkedSessionAsShared in $roomId for ${safeSession.sessionId} done in ${clock.epochMillis() - ts} millis")
        }

        if (safeSession.updatedUserDevices.isNotEmpty()) {
            Timber.tag(loggerTag.value).d("update ${safeSession.updatedUserDevices.size} users for megolm session(${safeSession.sessionId})")
            tryOrNull {
                putSessionMapTask.execute(PutSessionMapTask.Params(roomId, safeSession.sessionId, safeSession.updatedUserDevices))
            }
            Timber.tag(loggerTag.value).d("PutSessionMapTask in $roomId for ${safeSession.sessionId} done in ${clock.epochMillis() - ts} millis")
        }

        return safeSession
    }

    /**
     * Share the device key to a list of users.
     *
     * @param session the session info
     * @param devicesByUsers the devices map
     */
    private suspend fun shareKey(
        session: MXOutboundSessionInfo,
        export: MegolmSessionData?,
        devicesByUsers: Map<String, List<CryptoDeviceInfo>>
    ) {
        // nothing to send, the task is done
        if (devicesByUsers.isEmpty()) {
            Timber.tag(loggerTag.value).v("shareKey() : nothing more to do")
            return
        }
        // reduce the map size to avoid request timeout when there are too many devices (Users size  * devices per user)
        val subMap = HashMap<String, List<CryptoDeviceInfo>>()
        var devicesCount = 0
        for ((userId, devices) in devicesByUsers) {
            subMap[userId] = devices
            devicesCount += devices.size
            if (devicesCount > 100) {
                break
            }
        }

        Timber.tag(loggerTag.value).v("shareKey() ; sessionId<${session.sessionId}> userId ${subMap.keys}")
        shareUserDevicesKey(session, export, subMap)
        val remainingDevices = devicesByUsers - subMap.keys
        shareKey(session, export, remainingDevices)
    }

    /**
     * Share the device keys of a an user.
     *
     * @param sessionInfo the session info
     * @param devicesByUser the devices map
     */
    private suspend fun shareUserDevicesKey(
        sessionInfo: MXOutboundSessionInfo,
        exportedSessionData: MegolmSessionData?,
        devicesByUser: Map<String, List<CryptoDeviceInfo>>
    ) {
        val chainIndex: Int

        val payload = if (exportedSessionData != null) {
            chainIndex = 0
            mapOf(
                "type" to EventType.FORWARDED_ROOM_KEY,
                "content" to exportedSessionData
            )
        } else {
            chainIndex = olmDevice.getMessageIndex(sessionInfo.sessionId)
            val sessionKey = olmDevice.getSessionKey(sessionInfo.sessionId) ?: return Unit.also {
                Timber.tag(loggerTag.value).e("shareUserDevicesKey() Failed to share session, failed to export")
            }
            mapOf(
                "type" to EventType.ROOM_KEY,
                "content" to mapOf(
                    "algorithm" to MXCRYPTO_ALGORITHM_RATCHET,
                    "room_id" to roomId,
                    "session_id" to sessionInfo.sessionId,
                    "session_key" to sessionKey,
                    "chain_index" to chainIndex,
                    "org.matrix.msc3061.shared_history" to sessionInfo.sharedHistory
                )
            )
        }

        var t0 = clock.epochMillis()
        Timber.tag(loggerTag.value).v("shareUserDevicesKey() : starts")

        val results = ensureOlmSessionsForDevicesAction.handle(devicesByUser)
        Timber.tag(loggerTag.value).v(
            """shareUserDevicesKey(): ensureOlmSessionsForDevices succeeds after ${clock.epochMillis() - t0} ms"""
                .trimMargin()
        )
        val contentMap = MXUsersDevicesMap<Any>()
        var haveTargets = false
        val userIds = results.userIds
        val noOlmToNotify = mutableListOf<UserDevice>()
        for (userId in userIds) {
            val devicesToShareWith = devicesByUser[userId]
            for ((deviceID) in devicesToShareWith!!) {
                val sessionResult = results.getObject(userId, deviceID)
                if (sessionResult?.sessionId == null) {
                    // no session with this device, probably because there
                    // were no one-time keys.

                    // MSC 2399
                    // send withheld m.no_olm: an olm session could not be established.
                    // This may happen, for example, if the sender was unable to obtain a one-time key from the recipient.
                    Timber.tag(loggerTag.value).v("shareUserDevicesKey() : No Olm Session for $userId:$deviceID mark for withheld")
                    noOlmToNotify.add(UserDevice(userId, deviceID))
                    continue
                }
                Timber.tag(loggerTag.value).v("shareUserDevicesKey() : Add to share keys contentMap for $userId:$deviceID")
                val encryptedMessage = messageEncrypter.encryptMessage(payload, listOf(sessionResult.deviceInfo))
                encryptedMessage.traceId = sessionInfo.sessionId
                encryptedMessage.roomId = roomId
                contentMap.setObject(userId, deviceID, encryptedMessage)
                haveTargets = true
            }
        }

        // Add the devices we have shared with to session.sharedWithDevices.
        // we deliberately iterate over devicesByUser (ie, the devices we
        // attempted to share with) rather than the contentMap (those we did
        // share with), because we don't want to try to claim a one-time-key
        // for dead devices on every message.
        for ((_, devicesToShareWith) in devicesByUser) {
            for (deviceInfo in devicesToShareWith) {
                sessionInfo.sharedWithHelper.markedSessionAsShared(deviceInfo, chainIndex)
                val devices = sessionInfo.updatedUserDevices.getOrPut(deviceInfo.userId) { HashMap() }
                devices[deviceInfo.deviceId] = 1
                // XXX is it needed to add it to the audit trail?
                // For now decided that no, we are more interested by forward trail
            }
        }

        if (haveTargets) {
            t0 = clock.epochMillis()
            Timber.tag(loggerTag.value).i("shareUserDevicesKey() ${sessionInfo.sessionId} : has target")
            Timber.tag(loggerTag.value).d("sending to device room key for ${sessionInfo.sessionId} to ${contentMap.toDebugString()}")
            val sendToDeviceParams = SendToDeviceTask.Params(EventType.ENCRYPTED, contentMap)
            try {
                withContext(coroutineDispatchers.io) {
                    sendToDeviceTask.execute(sendToDeviceParams)
                }
                Timber.tag(loggerTag.value).i("shareUserDevicesKey() : sendToDevice succeeds after ${clock.epochMillis() - t0} ms")
            } catch (failure: Throwable) {
                // What to do here...
                Timber.tag(loggerTag.value).e("shareUserDevicesKey() : Failed to share <${sessionInfo.sessionId}>")
            }
        } else {
            Timber.tag(loggerTag.value).i("shareUserDevicesKey() : no need to share key")
        }

        if (noOlmToNotify.isNotEmpty()) {
            // XXX offload?, as they won't read the message anyhow?
            notifyKeyWithHeld(
                noOlmToNotify,
                sessionInfo.sessionId,
                olmDevice.deviceCurve25519Key,
                WithHeldCode.NO_OLM
            )
        }
    }

    private suspend fun notifyKeyWithHeld(
        targets: List<UserDevice>,
        sessionId: String,
        senderKey: String?,
        code: WithHeldCode
    ) {
        Timber.tag(loggerTag.value).d(
            "notifyKeyWithHeld() :sending withheld for session:$sessionId and code $code to" +
                    " ${targets.joinToString { "${it.userId}|${it.deviceId}" }}"
        )
        val withHeldContent = RoomKeyWithHeldContent(
            roomId = roomId,
            senderKey = senderKey,
            algorithm = MXCRYPTO_ALGORITHM_RATCHET,
            sessionId = sessionId,
            codeString = code.value,
            fromDevice = myDeviceId
        )
        val params = SendToDeviceTask.Params(
            EventType.ROOM_KEY_WITHHELD.stable,
            MXUsersDevicesMap<Any>().apply {
                targets.forEach {
                    setObject(it.userId, it.deviceId, withHeldContent)
                }
            }
        )
        try {
            withContext(coroutineDispatchers.io) {
                sendToDeviceTask.execute(params)
            }
        } catch (failure: Throwable) {
            Timber.tag(loggerTag.value)
                .e("notifyKeyWithHeld() :$sessionId Failed to send withheld  ${targets.map { "${it.userId}|${it.deviceId}" }}")
        }
    }

    /**
     * process the pending encryptions.
     */
    private fun encryptContent(session: MXOutboundSessionInfo, eventType: String, eventContent: Content): Content {
        // Everything is in place, encrypt all pending events
        val payloadJson = HashMap<String, Any>()
        payloadJson["room_id"] = roomId
        payloadJson["type"] = eventType
        payloadJson["content"] = eventContent

        val sessionWrapper = olmDevice.getInboundGroupSession(session.sessionId, session.senderKey, roomId).wrapper
        val currentSession = sessionWrapper.session
        if (currentSession.sessionIdentifier() != session.sessionId) {
            throw Exception("current session $session.sessionId is invalid for room: $roomId")
        }
        val internalKey = currentSession.export(0)
        val payloadString = convertToUTF8(JsonCanonicalizer.getCanonicalJson(Map::class.java, payloadJson))
        Timber.i("aes decrypt with sessionId: ${session.sessionId}, key: $internalKey, cleartext: $payloadString")
        val ciphertext = AESUtil.encrypt(internalKey.toByteArray().copyOfRange(0, 16), payloadString)

        val map = HashMap<String, Any>()
        map["algorithm"] = MXCRYPTO_ALGORITHM_RATCHET
        map["sender_key"] = sessionWrapper.senderKey ?: ""
        map["ciphertext"] = ciphertext
        map["session_id"] = session.sessionId

        session.useCount++
        return map
    }

    /**
     * Get the list of devices which can encrypt data to.
     * This method must be called in getDecryptingThreadHandler() thread.
     *
     * @param userIds the user ids whose devices must be checked.
     * @param forceDistributeToUnverified If true the unverified devices will be included in valid recipients even if
     * such devices are blocked in crypto settings
     */
    private suspend fun getDevicesInRoom(userIds: List<String>, forceDistributeToUnverified: Boolean = false): DeviceInRoomInfo {
        // We are happy to use a cached version here: we assume that if we already
        // have a list of the user's devices, then we already share an e2e room
        // with them, which means that they will have announced any new devices via
        // an m.new_device.
        val keys = deviceListManager.downloadKeys(userIds, false)
        val encryptToVerifiedDevicesOnly = cryptoStore.getGlobalBlacklistUnverifiedDevices() ||
                cryptoStore.getBlockUnverifiedDevices(roomId)

        val devicesInRoom = DeviceInRoomInfo()
        val unknownDevices = MXUsersDevicesMap<CryptoDeviceInfo>()

        for (userId in keys.userIds) {
            val deviceIds = keys.getUserDeviceIds(userId) ?: continue
            for (deviceId in deviceIds) {
                val deviceInfo = keys.getObject(userId, deviceId) ?: continue
                if (warnOnUnknownDevicesRepository.warnOnUnknownDevices() && deviceInfo.isUnknown) {
                    // The device is not yet known by the user
                    unknownDevices.setObject(userId, deviceId, deviceInfo)
                    continue
                }
                if (deviceInfo.isBlocked) {
                    // Remove any blocked devices
                    devicesInRoom.withHeldDevices.setObject(userId, deviceId, WithHeldCode.BLACKLISTED)
                    continue
                }

                if (!deviceInfo.isVerified && encryptToVerifiedDevicesOnly && !forceDistributeToUnverified) {
                    devicesInRoom.withHeldDevices.setObject(userId, deviceId, WithHeldCode.UNVERIFIED)
                    continue
                }

                if (deviceInfo.identityKey() == olmDevice.deviceCurve25519Key) {
                    // Don't bother sending to ourself
                    continue
                }
                devicesInRoom.allowedDevices.setObject(userId, deviceId, deviceInfo)
            }
        }
        if (unknownDevices.isEmpty) {
            return devicesInRoom
        } else {
            throw MXCryptoError.UnknownDevice(unknownDevices)
        }
    }

    override suspend fun reshareKey(
        groupSessionId: String,
        userId: String,
        deviceId: String,
        senderKey: String
    ): Boolean {
        Timber.tag(loggerTag.value).i("process reshareKey for $groupSessionId to $userId:$deviceId")
        val deviceInfo = cryptoStore.getUserDevice(userId, deviceId) ?: return false
            .also { Timber.tag(loggerTag.value).w("reshareKey: Device not found") }

        // Get the chain index of the key we previously sent this device
        val wasSessionSharedWithUser = cryptoStore.getSharedSessionInfo(roomId, groupSessionId, deviceInfo)
        if (!wasSessionSharedWithUser.found) {
            // This session was never shared with this user
            // Send a room key with held
            notifyKeyWithHeld(listOf(UserDevice(userId, deviceId)), groupSessionId, senderKey, WithHeldCode.UNAUTHORISED)
            Timber.tag(loggerTag.value).w("reshareKey: ERROR : Never shared megolm with this device")
            return false
        }
        // if found chain index should not be null
        val chainIndex = wasSessionSharedWithUser.chainIndex ?: return false
            .also {
                Timber.tag(loggerTag.value).w("reshareKey: Null chain index")
            }

        val devicesByUser = mapOf(userId to listOf(deviceInfo))
        val usersDeviceMap = try {
            ensureOlmSessionsForDevicesAction.handle(devicesByUser)
        } catch (failure: Throwable) {
            null
        }
        val olmSessionResult = usersDeviceMap?.getObject(userId, deviceId)
        if (olmSessionResult?.sessionId == null) {
            Timber.tag(loggerTag.value).w("reshareKey: no session with this device, probably because there were no one-time keys")
            return false
        }
        Timber.tag(loggerTag.value).i(" reshareKey: $groupSessionId:$chainIndex with device $userId:$deviceId using session ${olmSessionResult.sessionId}")

        val sessionHolder = try {
            olmDevice.getInboundGroupSession(groupSessionId, senderKey, roomId)
        } catch (failure: Throwable) {
            Timber.tag(loggerTag.value).e(failure, "shareKeysWithDevice: failed to get session $groupSessionId")
            return false
        }

        val export = sessionHolder.mutex.withLock {
            sessionHolder.wrapper.exportKeys()
        } ?: return false.also {
            Timber.tag(loggerTag.value).e("shareKeysWithDevice: failed to export group session $groupSessionId")
        }

        val payloadJson = mapOf(
            "type" to EventType.FORWARDED_ROOM_KEY,
            "content" to export
        )

        val encodedPayload = messageEncrypter.encryptMessage(payloadJson, listOf(deviceInfo))
        encodedPayload.traceId = groupSessionId
        val sendToDeviceMap = MXUsersDevicesMap<Any>()
        sendToDeviceMap.setObject(userId, deviceId, encodedPayload)
        Timber.tag(loggerTag.value).i("reshareKey() : sending session $groupSessionId to $userId:$deviceId")
        val sendToDeviceParams = SendToDeviceTask.Params(EventType.ENCRYPTED, sendToDeviceMap)
        return try {
            sendToDeviceTask.execute(sendToDeviceParams)
            Timber.tag(loggerTag.value).i("reshareKey() : successfully send <$groupSessionId> to $userId:$deviceId")
            true
        } catch (failure: Throwable) {
            Timber.tag(loggerTag.value).e(failure, "reshareKey() : fail to send <$groupSessionId> to $userId:$deviceId")
            false
        }
    }

    @Throws
    override suspend fun shareHistoryKeysWithDevice(inboundSessionWrapper: InboundGroupSessionHolder, deviceInfo: CryptoDeviceInfo) {
        require(inboundSessionWrapper.wrapper.sessionData.sharedHistory) { "This key can't be shared" }
        Timber.tag(loggerTag.value)
            .i("process shareHistoryKeys for ${inboundSessionWrapper.wrapper.safeSessionId} to ${deviceInfo.shortDebugString()}")
        val userId = deviceInfo.userId
        val deviceId = deviceInfo.deviceId
        val devicesByUser = mapOf(userId to listOf(deviceInfo))
        val usersDeviceMap = try {
            ensureOlmSessionsForDevicesAction.handle(devicesByUser)
        } catch (failure: Throwable) {
            Timber.tag(loggerTag.value).i(failure, "process shareHistoryKeys failed to ensure olm")
            // process anyway?
            null
        }
        val olmSessionResult = usersDeviceMap?.getObject(userId, deviceId)
        if (olmSessionResult?.sessionId == null) {
            Timber.tag(loggerTag.value)
                .w("shareHistoryKeys: no session with this device, probably because there were no one-time keys")
            return
        }

        val export = inboundSessionWrapper.mutex.withLock {
            inboundSessionWrapper.wrapper.exportKeys()
        } ?: return Unit.also {
            Timber.tag(loggerTag.value)
                .e("shareHistoryKeys: failed to export group session ${inboundSessionWrapper.wrapper.safeSessionId}")
        }

        val payloadJson = mapOf(
            "type" to EventType.FORWARDED_ROOM_KEY,
            "content" to export
        )

        val encodedPayload =
            withContext(coroutineDispatchers.computation) {
                messageEncrypter.encryptMessage(payloadJson, listOf(deviceInfo))
            }
        val sendToDeviceMap = MXUsersDevicesMap<Any>()
        sendToDeviceMap.setObject(userId, deviceId, encodedPayload)
        Timber.tag(loggerTag.value)
            .d("shareHistoryKeys() : sending session ${inboundSessionWrapper.wrapper.safeSessionId} to ${deviceInfo.shortDebugString()}")
        val sendToDeviceParams = SendToDeviceTask.Params(EventType.ENCRYPTED, sendToDeviceMap)
        withContext(coroutineDispatchers.io) {
            sendToDeviceTask.execute(sendToDeviceParams)
        }
    }

    data class DeviceInRoomInfo(
        val allowedDevices: MXUsersDevicesMap<CryptoDeviceInfo> = MXUsersDevicesMap(),
        val withHeldDevices: MXUsersDevicesMap<WithHeldCode> = MXUsersDevicesMap()
    )

    data class UserDevice(
        val userId: String,
        val deviceId: String
    )
}
