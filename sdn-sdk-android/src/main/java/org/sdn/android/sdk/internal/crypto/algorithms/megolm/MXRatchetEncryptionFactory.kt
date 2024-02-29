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
import org.sdn.android.sdk.api.SDNCoroutineDispatchers
import org.sdn.android.sdk.internal.crypto.DeviceListManager
import org.sdn.android.sdk.internal.crypto.MXOlmDevice
import org.sdn.android.sdk.internal.crypto.actions.EnsureOlmSessionsForDevicesAction
import org.sdn.android.sdk.internal.crypto.actions.MessageEncrypter
import org.sdn.android.sdk.internal.crypto.keysbackup.DefaultKeysBackupService
import org.sdn.android.sdk.internal.crypto.repository.WarnOnUnknownDeviceRepository
import org.sdn.android.sdk.internal.crypto.store.IMXCryptoStore
import org.sdn.android.sdk.internal.crypto.tasks.SendToDeviceTask
import org.sdn.android.sdk.internal.di.DeviceId
import org.sdn.android.sdk.internal.di.UserId
import org.sdn.android.sdk.internal.util.time.Clock
import javax.inject.Inject

internal class MXRatchetEncryptionFactory @Inject constructor(
    private val olmDevice: MXOlmDevice,
    private val defaultKeysBackupService: DefaultKeysBackupService,
    private val cryptoStore: IMXCryptoStore,
    private val deviceListManager: DeviceListManager,
    private val ensureOlmSessionsForDevicesAction: EnsureOlmSessionsForDevicesAction,
    @UserId private val userId: String,
    @DeviceId private val deviceId: String?,
    private val sendToDeviceTask: SendToDeviceTask,
    private val messageEncrypter: MessageEncrypter,
    private val warnOnUnknownDevicesRepository: WarnOnUnknownDeviceRepository,
    private val coroutineDispatchers: SDNCoroutineDispatchers,
    private val cryptoCoroutineScope: CoroutineScope,
    private val clock: Clock,
) {

    fun create(roomId: String): MXRatchetEncryption {
        return MXRatchetEncryption(
            roomId = roomId,
            olmDevice = olmDevice,
            defaultKeysBackupService = defaultKeysBackupService,
            cryptoStore = cryptoStore,
            deviceListManager = deviceListManager,
            ensureOlmSessionsForDevicesAction = ensureOlmSessionsForDevicesAction,
            myUserId = userId,
            myDeviceId = deviceId!!,
            sendToDeviceTask = sendToDeviceTask,
            messageEncrypter = messageEncrypter,
            warnOnUnknownDevicesRepository = warnOnUnknownDevicesRepository,
            coroutineDispatchers = coroutineDispatchers,
            cryptoCoroutineScope = cryptoCoroutineScope,
            clock = clock,
        )
    }
}
