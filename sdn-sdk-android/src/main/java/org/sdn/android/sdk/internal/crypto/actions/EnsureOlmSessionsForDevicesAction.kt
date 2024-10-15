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

package org.sdn.android.sdk.internal.crypto.actions

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.sdn.android.sdk.api.SDNCoroutineDispatchers
import org.sdn.android.sdk.api.logger.LoggerTag
import org.sdn.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.sdn.android.sdk.api.session.crypto.model.MXUsersDevicesMap
import org.sdn.android.sdk.api.util.JsonDict
import org.sdn.android.sdk.internal.crypto.MXOlmDevice
import org.sdn.android.sdk.internal.crypto.model.MXKey
import org.sdn.android.sdk.internal.crypto.model.MXOlmSessionResult
import org.sdn.android.sdk.internal.crypto.tasks.ClaimOneTimeKeysForUsersDeviceTask
import org.sdn.android.sdk.internal.session.SessionScope
import timber.log.Timber
import javax.inject.Inject

private const val ONE_TIME_KEYS_RETRY_COUNT = 3

private val loggerTag = LoggerTag("EnsureOlmSessionsForDevicesAction", LoggerTag.CRYPTO)

@SessionScope
internal class EnsureOlmSessionsForDevicesAction @Inject constructor(
    private val olmDevice: MXOlmDevice,
    private val coroutineDispatchers: SDNCoroutineDispatchers,
    private val oneTimeKeysForUsersDeviceTask: ClaimOneTimeKeysForUsersDeviceTask
) {

    private val ensureMutex = Mutex()

    /**
     * We want to synchronize a bit here, because we are iterating to check existing olm session and
     * also adding some.
     */
    suspend fun handle(devicesByUser: Map<String, List<CryptoDeviceInfo>>, force: Boolean = false): MXUsersDevicesMap<MXOlmSessionResult> {
        ensureMutex.withLock {
            val results = MXUsersDevicesMap<MXOlmSessionResult>()
            val deviceList = devicesByUser.flatMap { it.value }
            Timber.tag(loggerTag.value)
                    .d("ensure olm forced:$force for ${deviceList.joinToString { it.shortDebugString() }}")

            val devicesToCreateSessionWith = mutableListOf<CryptoDeviceInfo>()
            val devicesToClaimKeyWith = mutableListOf<CryptoDeviceInfo>()
            deviceList.forEach { deviceInfo ->
                val deviceId = deviceInfo.deviceId
                val userId = deviceInfo.userId
                val key = deviceInfo.identityKey() ?: return@forEach Unit.also {
                    Timber.tag(loggerTag.value).w("Ignoring device ${deviceInfo.shortDebugString()} without identity key")
                }

                // is there a session that as been already used?
                val sessionId = olmDevice.getSessionId(key)
                if (force || sessionId.isNullOrEmpty()) {
                    Timber.tag(loggerTag.value).d("Found no existing olm session ${deviceInfo.shortDebugString()} add to claim list")
                    if (deviceInfo.fallbackKey().isNullOrEmpty()) {
                        devicesToClaimKeyWith.add(deviceInfo)
                    }
                    devicesToCreateSessionWith.add(deviceInfo)
                } else {
                    Timber.tag(loggerTag.value).d("using olm session $sessionId for (${deviceInfo.userId}|$deviceId)")
                    val olmSessionResult = MXOlmSessionResult(deviceInfo, sessionId)
                    results.setObject(userId, deviceId, olmSessionResult)
                }
            }

            if (devicesToCreateSessionWith.isEmpty()) {
                // no session to create
                return results
            }

            // Let's now claim one time keys
            var oneTimeKeys = MXUsersDevicesMap<MXKey>()
            if (devicesToClaimKeyWith.isNotEmpty()) {
                val usersDevicesToClaim = MXUsersDevicesMap<String>().apply {
                    devicesToClaimKeyWith.forEach {
                        setObject(it.userId, it.deviceId, MXKey.KEY_SIGNED_CURVE_25519_TYPE)
                    }
                }
                val claimParams = ClaimOneTimeKeysForUsersDeviceTask.Params(usersDevicesToClaim)
                oneTimeKeys = withContext(coroutineDispatchers.io) {
                    oneTimeKeysForUsersDeviceTask.executeRetry(claimParams, ONE_TIME_KEYS_RETRY_COUNT)
                }
            }

            // let now start olm session using the new otks
            devicesToCreateSessionWith.forEach { deviceInfo ->
                val userId = deviceInfo.userId
                val deviceId = deviceInfo.deviceId
                val fbk = deviceInfo.fallbackKey()
                // Did we get an OTK
                var oneTimeKey = oneTimeKeys.getObject(userId, deviceId)
                if (oneTimeKey == null && !fbk.isNullOrEmpty()) {
                    oneTimeKey = MXKey(
                        type = MXKey.KEY_SIGNED_CURVE_25519_TYPE,
                        keyId = "",
                        value = fbk,
                        signatures = emptyMap(),
                        rawMap = emptyMap()
                    )
                }
                if (oneTimeKey == null) {
                    Timber.tag(loggerTag.value).d("No otk for ${deviceInfo.shortDebugString()}")
                } else if (oneTimeKey.type != MXKey.KEY_SIGNED_CURVE_25519_TYPE) {
                    Timber.tag(loggerTag.value).d("Bad otk type (${oneTimeKey.type}) for ${deviceInfo.shortDebugString()}")
                } else {
                    val olmSessionId = verifyKeyAndStartSession(oneTimeKey, userId, deviceInfo)
                    if (olmSessionId != null) {
                        val olmSessionResult = MXOlmSessionResult(deviceInfo, olmSessionId)
                        results.setObject(userId, deviceId, olmSessionResult)
                    } else {
                        Timber
                                .tag(loggerTag.value)
                                .d("## CRYPTO | cant unwedge failed to create outbound ${deviceInfo.shortDebugString()}")
                    }
                }
            }
            return results
        }
    }

    private fun verifyKeyAndStartSession(oneTimeKey: MXKey, userId: String, deviceInfo: CryptoDeviceInfo): String? {
        var sessionId: String? = null
        var isVerified = true
        var errorMessage: String? = null

        val deviceId = deviceInfo.deviceId
        val signKeyId = "ed25519:$deviceId"
        val signature = oneTimeKey.signatureForUserId(userId, signKeyId)
        val fingerprint = deviceInfo.fingerprint()

        if (!signature.isNullOrEmpty() && !fingerprint.isNullOrEmpty()) {
            try {
                olmDevice.verifySignature(fingerprint, oneTimeKey.signalableJSONDictionary(), signature)
            } catch (e: Exception) {
                Timber.tag(loggerTag.value).d(
                        e, "verifyKeyAndStartSession() : Verify error for otk: ${oneTimeKey.signalableJSONDictionary()}," +
                        " signature:$signature fingerprint:$fingerprint"
                )
                Timber.tag(loggerTag.value).e(
                        "verifyKeyAndStartSession() : Verify error for ${deviceInfo.userId}|${deviceInfo.deviceId} " +
                                " - signable json ${oneTimeKey.signalableJSONDictionary()}"
                )
                isVerified = false
                errorMessage = e.message
            }
        }
        // Check one-time key signature
        if (isVerified) {
            sessionId = deviceInfo.identityKey()?.let { identityKey ->
                olmDevice.createOutboundSession(identityKey, oneTimeKey.value)
            }

            if (sessionId.isNullOrEmpty()) {
                // Possibly a bad key
                Timber.tag(loggerTag.value).e("verifyKeyAndStartSession() : Error starting session with device $userId:$deviceId")
            } else {
                Timber.tag(loggerTag.value).d("verifyKeyAndStartSession() : Started new sessionId $sessionId for device $userId:$deviceId")
            }
        } else {
            Timber.tag(loggerTag.value).e("verifyKeyAndStartSession() : Unable to verify otk signature for $userId:$deviceId: $errorMessage")
        }

        return sessionId
    }
}
