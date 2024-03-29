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

package org.sdn.android.sdk.internal.crypto.crosssigning

import androidx.lifecycle.LiveData
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.sdn.android.sdk.api.SDNCallback
import org.sdn.android.sdk.api.SDNCoroutineDispatchers
import org.sdn.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.sdn.android.sdk.api.extensions.orFalse
import org.sdn.android.sdk.api.session.crypto.crosssigning.CrossSigningService
import org.sdn.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.sdn.android.sdk.api.session.crypto.crosssigning.DeviceTrustResult
import org.sdn.android.sdk.api.session.crypto.crosssigning.MXCrossSigningInfo
import org.sdn.android.sdk.api.session.crypto.crosssigning.PrivateKeysInfo
import org.sdn.android.sdk.api.session.crypto.crosssigning.UserTrustResult
import org.sdn.android.sdk.api.session.crypto.crosssigning.isCrossSignedVerified
import org.sdn.android.sdk.api.session.crypto.crosssigning.isLocallyVerified
import org.sdn.android.sdk.api.session.crypto.crosssigning.isVerified
import org.sdn.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.sdn.android.sdk.api.util.Optional
import org.sdn.android.sdk.api.util.fromBase64
import org.sdn.android.sdk.internal.crypto.DeviceListManager
import org.sdn.android.sdk.internal.crypto.OutgoingKeyRequestManager
import org.sdn.android.sdk.internal.crypto.model.rest.UploadSignatureQueryBuilder
import org.sdn.android.sdk.internal.crypto.store.IMXCryptoStore
import org.sdn.android.sdk.internal.crypto.tasks.InitializeCrossSigningTask
import org.sdn.android.sdk.internal.crypto.tasks.UploadSignaturesTask
import org.sdn.android.sdk.internal.di.SessionId
import org.sdn.android.sdk.internal.di.UserId
import org.sdn.android.sdk.internal.di.WorkManagerProvider
import org.sdn.android.sdk.internal.session.SessionScope
import org.sdn.android.sdk.internal.task.TaskExecutor
import org.sdn.android.sdk.internal.task.TaskThread
import org.sdn.android.sdk.internal.task.configureWith
import org.sdn.android.sdk.internal.util.JsonCanonicalizer
import org.sdn.android.sdk.internal.util.logLimit
import org.sdn.android.sdk.internal.worker.WorkerParamsFactory
import org.matrix.olm.OlmPkSigning
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@SessionScope
internal class DefaultCrossSigningService @Inject constructor(
    @UserId private val myUserId: String,
    @SessionId private val sessionId: String,
    private val cryptoStore: IMXCryptoStore,
    private val deviceListManager: DeviceListManager,
    private val initializeCrossSigningTask: InitializeCrossSigningTask,
    private val uploadSignaturesTask: UploadSignaturesTask,
    private val taskExecutor: TaskExecutor,
    private val coroutineDispatchers: SDNCoroutineDispatchers,
    private val cryptoCoroutineScope: CoroutineScope,
    private val workManagerProvider: WorkManagerProvider,
    private val outgoingKeyRequestManager: OutgoingKeyRequestManager,
    private val crossSigningOlm: CrossSigningOlm,
    private val updateTrustWorkerDataRepository: UpdateTrustWorkerDataRepository
) : CrossSigningService,
        DeviceListManager.UserDevicesUpdateListener {

    init {
        try {

            // Try to get stored keys if they exist
            cryptoStore.getMyCrossSigningInfo()?.let { mxCrossSigningInfo ->
                Timber.i("## CrossSigning - Found Existing self signed keys")
                Timber.i("## CrossSigning - Checking if private keys are known")

                cryptoStore.getCrossSigningPrivateKeys()?.let { privateKeysInfo ->
                    privateKeysInfo.master
                            ?.fromBase64()
                            ?.let { privateKeySeed ->
                                val pkSigning = OlmPkSigning()
                                if (pkSigning.initWithSeed(privateKeySeed) == mxCrossSigningInfo.masterKey()?.unpaddedBase64PublicKey) {
                                    crossSigningOlm.masterPkSigning = pkSigning
                                    Timber.i("## CrossSigning - Loading master key success")
                                } else {
                                    Timber.w("## CrossSigning - Public master key does not match the private key")
                                    pkSigning.releaseSigning()
                                    // TODO untrust?
                                }
                            }
                    privateKeysInfo.user
                            ?.fromBase64()
                            ?.let { privateKeySeed ->
                                val pkSigning = OlmPkSigning()
                                if (pkSigning.initWithSeed(privateKeySeed) == mxCrossSigningInfo.userKey()?.unpaddedBase64PublicKey) {
                                    crossSigningOlm.userPkSigning = pkSigning
                                    Timber.i("## CrossSigning - Loading User Signing key success")
                                } else {
                                    Timber.w("## CrossSigning - Public User key does not match the private key")
                                    pkSigning.releaseSigning()
                                    // TODO untrust?
                                }
                            }
                    privateKeysInfo.selfSigned
                            ?.fromBase64()
                            ?.let { privateKeySeed ->
                                val pkSigning = OlmPkSigning()
                                if (pkSigning.initWithSeed(privateKeySeed) == mxCrossSigningInfo.selfSigningKey()?.unpaddedBase64PublicKey) {
                                    crossSigningOlm.selfSigningPkSigning = pkSigning
                                    Timber.i("## CrossSigning - Loading Self Signing key success")
                                } else {
                                    Timber.w("## CrossSigning - Public Self Signing key does not match the private key")
                                    pkSigning.releaseSigning()
                                    // TODO untrust?
                                }
                            }
                }

                // Recover local trust in case private key are there?
                setUserKeysAsTrusted(myUserId, checkUserTrust(myUserId).isVerified())
            }
        } catch (e: Throwable) {
            // Mmm this kind of a big issue
            Timber.e(e, "Failed to initialize Cross Signing")
        }

        deviceListManager.addListener(this)
    }

    fun release() {
        crossSigningOlm.release()
        deviceListManager.removeListener(this)
    }

    protected fun finalize() {
        release()
    }

    /**
     * - Make 3 key pairs (MSK, USK, SSK)
     * - Save the private keys with proper security
     * - Sign the keys and upload them
     * - Sign the current device with SSK and sign MSK with device key (migration) and upload signatures.
     */
    override fun initializeCrossSigning(uiaInterceptor: UserInteractiveAuthInterceptor?, callback: SDNCallback<Unit>) {
        Timber.d("## CrossSigning  initializeCrossSigning")

        val params = InitializeCrossSigningTask.Params(
                interactiveAuthInterceptor = uiaInterceptor
        )
        initializeCrossSigningTask.configureWith(params) {
            this.callbackThread = TaskThread.CRYPTO
            this.callback = object : SDNCallback<InitializeCrossSigningTask.Result> {
                override fun onFailure(failure: Throwable) {
                    Timber.e(failure, "Error in initializeCrossSigning()")
                    callback.onFailure(failure)
                }

                override fun onSuccess(data: InitializeCrossSigningTask.Result) {
                    val crossSigningInfo = MXCrossSigningInfo(
                            myUserId,
                            listOf(data.masterKeyInfo, data.userKeyInfo, data.selfSignedKeyInfo),
                            true
                    )
                    cryptoStore.setMyCrossSigningInfo(crossSigningInfo)
                    setUserKeysAsTrusted(myUserId, true)
                    cryptoStore.storePrivateKeysInfo(data.masterKeyPK, data.userKeyPK, data.selfSigningKeyPK)
                    crossSigningOlm.masterPkSigning = OlmPkSigning().apply { initWithSeed(data.masterKeyPK.fromBase64()) }
                    crossSigningOlm.userPkSigning = OlmPkSigning().apply { initWithSeed(data.userKeyPK.fromBase64()) }
                    crossSigningOlm.selfSigningPkSigning = OlmPkSigning().apply { initWithSeed(data.selfSigningKeyPK.fromBase64()) }

                    callback.onSuccess(Unit)
                }
            }
        }.executeBy(taskExecutor)
    }

    override fun onSecretMSKGossip(mskPrivateKey: String) {
        Timber.i("## CrossSigning - onSecretSSKGossip")
        val mxCrossSigningInfo = getMyCrossSigningKeys() ?: return Unit.also {
            Timber.e("## CrossSigning - onSecretMSKGossip() received secret but public key is not known")
        }

        mskPrivateKey.fromBase64()
                .let { privateKeySeed ->
                    val pkSigning = OlmPkSigning()
                    try {
                        if (pkSigning.initWithSeed(privateKeySeed) == mxCrossSigningInfo.masterKey()?.unpaddedBase64PublicKey) {
                            crossSigningOlm.masterPkSigning?.releaseSigning()
                            crossSigningOlm.masterPkSigning = pkSigning
                            Timber.i("## CrossSigning - Loading MSK success")
                            cryptoStore.storeMSKPrivateKey(mskPrivateKey)
                            return
                        } else {
                            Timber.e("## CrossSigning - onSecretMSKGossip() private key do not match public key")
                            pkSigning.releaseSigning()
                        }
                    } catch (failure: Throwable) {
                        Timber.e("## CrossSigning - onSecretMSKGossip() ${failure.localizedMessage}")
                        pkSigning.releaseSigning()
                    }
                }
    }

    override fun onSecretSSKGossip(sskPrivateKey: String) {
        Timber.i("## CrossSigning - onSecretSSKGossip")
        val mxCrossSigningInfo = getMyCrossSigningKeys() ?: return Unit.also {
            Timber.e("## CrossSigning - onSecretSSKGossip() received secret but public key is not known")
        }

        sskPrivateKey.fromBase64()
                .let { privateKeySeed ->
                    val pkSigning = OlmPkSigning()
                    try {
                        if (pkSigning.initWithSeed(privateKeySeed) == mxCrossSigningInfo.selfSigningKey()?.unpaddedBase64PublicKey) {
                            crossSigningOlm.selfSigningPkSigning?.releaseSigning()
                            crossSigningOlm.selfSigningPkSigning = pkSigning
                            Timber.i("## CrossSigning - Loading SSK success")
                            cryptoStore.storeSSKPrivateKey(sskPrivateKey)
                            return
                        } else {
                            Timber.e("## CrossSigning - onSecretSSKGossip() private key do not match public key")
                            pkSigning.releaseSigning()
                        }
                    } catch (failure: Throwable) {
                        Timber.e("## CrossSigning - onSecretSSKGossip() ${failure.localizedMessage}")
                        pkSigning.releaseSigning()
                    }
                }
    }

    override fun onSecretUSKGossip(uskPrivateKey: String) {
        Timber.i("## CrossSigning - onSecretUSKGossip")
        val mxCrossSigningInfo = getMyCrossSigningKeys() ?: return Unit.also {
            Timber.e("## CrossSigning - onSecretUSKGossip() received secret but public key is not knwow ")
        }

        uskPrivateKey.fromBase64()
                .let { privateKeySeed ->
                    val pkSigning = OlmPkSigning()
                    try {
                        if (pkSigning.initWithSeed(privateKeySeed) == mxCrossSigningInfo.userKey()?.unpaddedBase64PublicKey) {
                            crossSigningOlm.userPkSigning?.releaseSigning()
                            crossSigningOlm.userPkSigning = pkSigning
                            Timber.i("## CrossSigning - Loading USK success")
                            cryptoStore.storeUSKPrivateKey(uskPrivateKey)
                            return
                        } else {
                            Timber.e("## CrossSigning - onSecretUSKGossip() private key do not match public key")
                            pkSigning.releaseSigning()
                        }
                    } catch (failure: Throwable) {
                        pkSigning.releaseSigning()
                    }
                }
    }

    override fun checkTrustFromPrivateKeys(
            masterKeyPrivateKey: String?,
            uskKeyPrivateKey: String?,
            sskPrivateKey: String?
    ): UserTrustResult {
        val mxCrossSigningInfo = getMyCrossSigningKeys() ?: return UserTrustResult.CrossSigningNotConfigured(myUserId)

        var masterKeyIsTrusted = false
        var userKeyIsTrusted = false
        var selfSignedKeyIsTrusted = false

        masterKeyPrivateKey?.fromBase64()
                ?.let { privateKeySeed ->
                    val pkSigning = OlmPkSigning()
                    try {
                        if (pkSigning.initWithSeed(privateKeySeed) == mxCrossSigningInfo.masterKey()?.unpaddedBase64PublicKey) {
                            crossSigningOlm.masterPkSigning?.releaseSigning()
                            crossSigningOlm.masterPkSigning = pkSigning
                            masterKeyIsTrusted = true
                            Timber.i("## CrossSigning - Loading master key success")
                        } else {
                            pkSigning.releaseSigning()
                        }
                    } catch (failure: Throwable) {
                        pkSigning.releaseSigning()
                    }
                }

        uskKeyPrivateKey?.fromBase64()
                ?.let { privateKeySeed ->
                    val pkSigning = OlmPkSigning()
                    try {
                        if (pkSigning.initWithSeed(privateKeySeed) == mxCrossSigningInfo.userKey()?.unpaddedBase64PublicKey) {
                            crossSigningOlm.userPkSigning?.releaseSigning()
                            crossSigningOlm.userPkSigning = pkSigning
                            userKeyIsTrusted = true
                            Timber.i("## CrossSigning - Loading master key success")
                        } else {
                            pkSigning.releaseSigning()
                        }
                    } catch (failure: Throwable) {
                        pkSigning.releaseSigning()
                    }
                }

        sskPrivateKey?.fromBase64()
                ?.let { privateKeySeed ->
                    val pkSigning = OlmPkSigning()
                    try {
                        if (pkSigning.initWithSeed(privateKeySeed) == mxCrossSigningInfo.selfSigningKey()?.unpaddedBase64PublicKey) {
                            crossSigningOlm.selfSigningPkSigning?.releaseSigning()
                            crossSigningOlm.selfSigningPkSigning = pkSigning
                            selfSignedKeyIsTrusted = true
                            Timber.i("## CrossSigning - Loading master key success")
                        } else {
                            pkSigning.releaseSigning()
                        }
                    } catch (failure: Throwable) {
                        pkSigning.releaseSigning()
                    }
                }

        if (!masterKeyIsTrusted || !userKeyIsTrusted || !selfSignedKeyIsTrusted) {
            return UserTrustResult.KeysNotTrusted(mxCrossSigningInfo)
        } else {
            cryptoStore.markMyMasterKeyAsLocallyTrusted(true)
            val checkSelfTrust = checkSelfTrust()
            if (checkSelfTrust.isVerified()) {
                cryptoStore.storePrivateKeysInfo(masterKeyPrivateKey, uskKeyPrivateKey, sskPrivateKey)
                setUserKeysAsTrusted(myUserId, true)
            }
            return checkSelfTrust
        }
    }

    /**
     *
     *  ┏━━━━━━━━┓                             ┏━━━━━━━━┓
     *  ┃ ALICE  ┃                             ┃  BOB   ┃
     *  ┗━━━━━━━━┛                             ┗━━━━━━━━┛
     *   MSK                      ┌────────────▶  MSK
     *                            │
     *     │                      │
     *     │    SSK               │
     *     │                      │
     *     │                      │
     *     └──▶ USK   ────────────┘
     * .
     */
    override fun isUserTrusted(otherUserId: String): Boolean {
        return cryptoStore.getCrossSigningInfo(otherUserId)?.isTrusted() == true
    }

    override fun isCrossSigningVerified(): Boolean {
        return checkSelfTrust().isVerified()
    }

    /**
     * Will not force a download of the key, but will verify signatures trust chain.
     */
    override fun checkUserTrust(otherUserId: String): UserTrustResult {
        Timber.v("## CrossSigning  checkUserTrust for $otherUserId")
        if (otherUserId == myUserId) {
            return checkSelfTrust()
        }
        // I trust a user if I trust his master key
        // I can trust the master key if it is signed by my user key
        // TODO what if the master key is signed by a device key that i have verified

        // First let's get my user key
        val myCrossSigningInfo = cryptoStore.getCrossSigningInfo(myUserId)

        return checkOtherMSKTrusted(myCrossSigningInfo, cryptoStore.getCrossSigningInfo(otherUserId))
    }

    fun checkOtherMSKTrusted(myCrossSigningInfo: MXCrossSigningInfo?, otherInfo: MXCrossSigningInfo?): UserTrustResult {
        val myUserKey = myCrossSigningInfo?.userKey()
                ?: return UserTrustResult.CrossSigningNotConfigured(myUserId)

        if (!myCrossSigningInfo.isTrusted()) {
            return UserTrustResult.KeysNotTrusted(myCrossSigningInfo)
        }

        // Let's get the other user  master key
        val otherMasterKey = otherInfo?.masterKey()
                ?: return UserTrustResult.UnknownCrossSignatureInfo(otherInfo?.userId ?: "")

        val masterKeySignaturesMadeByMyUserKey = otherMasterKey.signatures
                ?.get(myUserId) // Signatures made by me
                ?.get("ed25519:${myUserKey.unpaddedBase64PublicKey}")

        if (masterKeySignaturesMadeByMyUserKey.isNullOrBlank()) {
            Timber.d("## CrossSigning  checkUserTrust false for ${otherInfo.userId}, not signed by my UserSigningKey")
            return UserTrustResult.KeyNotSigned(otherMasterKey)
        }

        // Check that Alice USK signature of Bob MSK is valid
        try {
            crossSigningOlm.olmUtility.verifyEd25519Signature(
                    masterKeySignaturesMadeByMyUserKey,
                    myUserKey.unpaddedBase64PublicKey,
                    otherMasterKey.canonicalSignable()
            )
        } catch (failure: Throwable) {
            return UserTrustResult.InvalidSignature(myUserKey, masterKeySignaturesMadeByMyUserKey)
        }

        return UserTrustResult.Success
    }

    private fun checkSelfTrust(): UserTrustResult {
        // Special case when it's me,
        // I have to check that MSK -> USK -> SSK
        // and that MSK is trusted (i know the private key, or is signed by a trusted device)
        val myCrossSigningInfo = cryptoStore.getCrossSigningInfo(myUserId)

        return checkSelfTrust(myCrossSigningInfo, cryptoStore.getUserDeviceList(myUserId))
    }

    fun checkSelfTrust(myCrossSigningInfo: MXCrossSigningInfo?, myDevices: List<CryptoDeviceInfo>?): UserTrustResult {
        // Special case when it's me,
        // I have to check that MSK -> USK -> SSK
        // and that MSK is trusted (i know the private key, or is signed by a trusted device)
//        val myCrossSigningInfo = cryptoStore.getCrossSigningInfo(userId)

        val myMasterKey = myCrossSigningInfo?.masterKey()
                ?: return UserTrustResult.CrossSigningNotConfigured(myUserId)

        // Is the master key trusted
        // 1) check if I know the private key
        val masterPrivateKey = cryptoStore.getCrossSigningPrivateKeys()
                ?.master
                ?.fromBase64()

        var isMaterKeyTrusted = false
        if (myMasterKey.trustLevel?.locallyVerified == true) {
            isMaterKeyTrusted = true
        } else if (masterPrivateKey != null) {
            // Check if private match public
            var olmPkSigning: OlmPkSigning? = null
            try {
                olmPkSigning = OlmPkSigning()
                val expectedPK = olmPkSigning.initWithSeed(masterPrivateKey)
                isMaterKeyTrusted = myMasterKey.unpaddedBase64PublicKey == expectedPK
            } catch (failure: Throwable) {
                Timber.e(failure)
            }
            olmPkSigning?.releaseSigning()
        } else {
            // Maybe it's signed by a locally trusted device?
            myMasterKey.signatures?.get(myUserId)?.forEach { (key, value) ->
                val potentialDeviceId = key.removePrefix("ed25519:")
                val potentialDevice = myDevices?.firstOrNull { it.deviceId == potentialDeviceId } // cryptoStore.getUserDevice(userId, potentialDeviceId)
                if (potentialDevice != null && potentialDevice.isVerified) {
                    // Check signature validity?
                    try {
                        crossSigningOlm.olmUtility.verifyEd25519Signature(value, potentialDevice.fingerprint(), myMasterKey.canonicalSignable())
                        isMaterKeyTrusted = true
                        return@forEach
                    } catch (failure: Throwable) {
                        // log
                        Timber.w(failure, "Signature not valid?")
                    }
                }
            }
        }

        if (!isMaterKeyTrusted) {
            return UserTrustResult.KeysNotTrusted(myCrossSigningInfo)
        }

        val myUserKey = myCrossSigningInfo.userKey()
                ?: return UserTrustResult.CrossSigningNotConfigured(myUserId)

        val userKeySignaturesMadeByMyMasterKey = myUserKey.signatures
                ?.get(myUserId) // Signatures made by me
                ?.get("ed25519:${myMasterKey.unpaddedBase64PublicKey}")

        if (userKeySignaturesMadeByMyMasterKey.isNullOrBlank()) {
            Timber.d("## CrossSigning  checkUserTrust false for $myUserId, USK not signed by MSK")
            return UserTrustResult.KeyNotSigned(myUserKey)
        }

        // Check that Alice USK signature of Alice MSK is valid
        try {
            crossSigningOlm.olmUtility.verifyEd25519Signature(
                    userKeySignaturesMadeByMyMasterKey,
                    myMasterKey.unpaddedBase64PublicKey,
                    myUserKey.canonicalSignable()
            )
        } catch (failure: Throwable) {
            return UserTrustResult.InvalidSignature(myUserKey, userKeySignaturesMadeByMyMasterKey)
        }

        val mySSKey = myCrossSigningInfo.selfSigningKey()
                ?: return UserTrustResult.CrossSigningNotConfigured(myUserId)

        val ssKeySignaturesMadeByMyMasterKey = mySSKey.signatures
                ?.get(myUserId) // Signatures made by me
                ?.get("ed25519:${myMasterKey.unpaddedBase64PublicKey}")

        if (ssKeySignaturesMadeByMyMasterKey.isNullOrBlank()) {
            Timber.d("## CrossSigning  checkUserTrust false for $myUserId, SSK not signed by MSK")
            return UserTrustResult.KeyNotSigned(mySSKey)
        }

        // Check that Alice USK signature of Alice MSK is valid
        try {
            crossSigningOlm.olmUtility.verifyEd25519Signature(
                    ssKeySignaturesMadeByMyMasterKey,
                    myMasterKey.unpaddedBase64PublicKey,
                    mySSKey.canonicalSignable()
            )
        } catch (failure: Throwable) {
            return UserTrustResult.InvalidSignature(mySSKey, ssKeySignaturesMadeByMyMasterKey)
        }

        return UserTrustResult.Success
    }

    override fun getUserCrossSigningKeys(otherUserId: String): MXCrossSigningInfo? {
        return cryptoStore.getCrossSigningInfo(otherUserId)
    }

    override fun getLiveCrossSigningKeys(userId: String): LiveData<Optional<MXCrossSigningInfo>> {
        return cryptoStore.getLiveCrossSigningInfo(userId)
    }

    override fun getMyCrossSigningKeys(): MXCrossSigningInfo? {
        return cryptoStore.getMyCrossSigningInfo()
    }

    override fun getCrossSigningPrivateKeys(): PrivateKeysInfo? {
        return cryptoStore.getCrossSigningPrivateKeys()
    }

    override fun getLiveCrossSigningPrivateKeys(): LiveData<Optional<PrivateKeysInfo>> {
        return cryptoStore.getLiveCrossSigningPrivateKeys()
    }

    override fun canCrossSign(): Boolean {
        return checkSelfTrust().isVerified() && cryptoStore.getCrossSigningPrivateKeys()?.selfSigned != null &&
                cryptoStore.getCrossSigningPrivateKeys()?.user != null
    }

    override fun allPrivateKeysKnown(): Boolean {
        return checkSelfTrust().isVerified() &&
                cryptoStore.getCrossSigningPrivateKeys()?.allKnown().orFalse()
    }

    override fun trustUser(otherUserId: String, callback: SDNCallback<Unit>) {
        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            Timber.d("## CrossSigning - Mark user $otherUserId as trusted ")
            // We should have this user keys
            val otherMasterKeys = getUserCrossSigningKeys(otherUserId)?.masterKey()
            if (otherMasterKeys == null) {
                callback.onFailure(Throwable("## CrossSigning - Other master signing key is not known"))
                return@launch
            }
            val myKeys = getUserCrossSigningKeys(myUserId)
            if (myKeys == null) {
                callback.onFailure(Throwable("## CrossSigning - CrossSigning is not setup for this account"))
                return@launch
            }
            val userPubKey = myKeys.userKey()?.unpaddedBase64PublicKey
            if (userPubKey == null || crossSigningOlm.userPkSigning == null) {
                callback.onFailure(Throwable("## CrossSigning - Cannot sign from this account, privateKeyUnknown $userPubKey"))
                return@launch
            }

            // Sign the other MasterKey with our UserSigning key
            val newSignature = JsonCanonicalizer.getCanonicalJson(
                    Map::class.java,
                    otherMasterKeys.signalableJSONDictionary()
            ).let { crossSigningOlm.userPkSigning?.sign(it) }

            if (newSignature == null) {
                // race??
                callback.onFailure(Throwable("## CrossSigning - Failed to sign"))
                return@launch
            }

            cryptoStore.setUserKeysAsTrusted(otherUserId, true)

            Timber.d("## CrossSigning - Upload signature of $otherUserId MSK signed by USK")
            val uploadQuery = UploadSignatureQueryBuilder()
                    .withSigningKeyInfo(otherMasterKeys.copyForSignature(myUserId, userPubKey, newSignature))
                    .build()
            uploadSignaturesTask.configureWith(UploadSignaturesTask.Params(uploadQuery)) {
                this.executionThread = TaskThread.CRYPTO
                this.callback = callback
            }.executeBy(taskExecutor)

            // Local echo for device cross trust, to avoid having to wait for a notification of key change
            cryptoStore.getUserDeviceList(otherUserId)?.forEach { device ->
                val updatedTrust = checkDeviceTrust(device.userId, device.deviceId, device.trustLevel?.isLocallyVerified() ?: false)
                Timber.v("## CrossSigning - update trust for device ${device.deviceId} of user $otherUserId , verified=$updatedTrust")
                cryptoStore.setDeviceTrust(device.userId, device.deviceId, updatedTrust.isCrossSignedVerified(), updatedTrust.isLocallyVerified())
            }
        }
    }

    override fun markMyMasterKeyAsTrusted() {
        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            cryptoStore.markMyMasterKeyAsLocallyTrusted(true)
            checkSelfTrust()
            // re-verify all trusts
            onUsersDeviceUpdate(listOf(myUserId))
        }
    }

    override fun trustDevice(deviceId: String, callback: SDNCallback<Unit>) {
        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            // This device should be yours
            val device = cryptoStore.getUserDevice(myUserId, deviceId)
            if (device == null) {
                callback.onFailure(IllegalArgumentException("This device [$deviceId] is not known, or not yours"))
                return@launch
            }

            val myKeys = getUserCrossSigningKeys(myUserId)
            if (myKeys == null) {
                callback.onFailure(Throwable("CrossSigning is not setup for this account"))
                return@launch
            }

            val ssPubKey = myKeys.selfSigningKey()?.unpaddedBase64PublicKey
            if (ssPubKey == null || crossSigningOlm.selfSigningPkSigning == null) {
                callback.onFailure(Throwable("Cannot sign from this account, public and/or privateKey Unknown $ssPubKey"))
                return@launch
            }

            // Sign with self signing
            val newSignature = crossSigningOlm.selfSigningPkSigning?.sign(device.canonicalSignable())

            if (newSignature == null) {
                // race??
                callback.onFailure(Throwable("Failed to sign"))
                return@launch
            }
            val toUpload = device.copy(
                    signatures = mapOf(
                            myUserId
                                    to
                                    mapOf(
                                            "ed25519:$ssPubKey" to newSignature
                                    )
                    )
            )

            val uploadQuery = UploadSignatureQueryBuilder()
                    .withDeviceInfo(toUpload)
                    .build()
            uploadSignaturesTask.configureWith(UploadSignaturesTask.Params(uploadQuery)) {
                this.executionThread = TaskThread.CRYPTO
                this.callback = callback
            }.executeBy(taskExecutor)
        }
    }

    override fun checkDeviceTrust(otherUserId: String, otherDeviceId: String, locallyTrusted: Boolean?): DeviceTrustResult {
        val otherDevice = cryptoStore.getUserDevice(otherUserId, otherDeviceId)
                ?: return DeviceTrustResult.UnknownDevice(otherDeviceId)

        val myKeys = getUserCrossSigningKeys(myUserId)
                ?: return legacyFallbackTrust(locallyTrusted, DeviceTrustResult.CrossSigningNotConfigured(myUserId))

        if (!myKeys.isTrusted()) return legacyFallbackTrust(locallyTrusted, DeviceTrustResult.KeysNotTrusted(myKeys))

        val otherKeys = getUserCrossSigningKeys(otherUserId)
                ?: return legacyFallbackTrust(locallyTrusted, DeviceTrustResult.CrossSigningNotConfigured(otherUserId))

        // TODO should we force verification ?
        if (!otherKeys.isTrusted()) return legacyFallbackTrust(locallyTrusted, DeviceTrustResult.KeysNotTrusted(otherKeys))

        // Check if the trust chain is valid
        /*
         *  ┏━━━━━━━━┓                             ┏━━━━━━━━┓
         *  ┃ ALICE  ┃                             ┃  BOB   ┃
         *  ┗━━━━━━━━┛                             ┗━━━━━━━━┛
         *   MSK                      ┌────────────▶MSK
         *                            │
         *     │                      │               │
         *     │    SSK               │               └──▶ SSK  ──────────────────┐
         *     │                      │                                           │
         *     │                      │                    USK                    │
         *     └──▶ USK   ────────────┘              (not visible by              │
         *                                                Alice)                  │
         *                                                                        ▼
         *                                                                ┌──────────────┐
         *                                                                │ BOB's Device │
         *                                                                └──────────────┘
         */

        val otherSSKSignature = otherDevice.signatures?.get(otherUserId)?.get("ed25519:${otherKeys.selfSigningKey()?.unpaddedBase64PublicKey}")
                ?: return legacyFallbackTrust(
                        locallyTrusted,
                        DeviceTrustResult.MissingDeviceSignature(
                                otherDeviceId, otherKeys.selfSigningKey()
                                ?.unpaddedBase64PublicKey
                                ?: ""
                        )
                )

        // Check  bob's device is signed by bob's SSK
        try {
            crossSigningOlm.olmUtility.verifyEd25519Signature(
                    otherSSKSignature,
                    otherKeys.selfSigningKey()?.unpaddedBase64PublicKey,
                    otherDevice.canonicalSignable()
            )
        } catch (e: Throwable) {
            return legacyFallbackTrust(locallyTrusted, DeviceTrustResult.InvalidDeviceSignature(otherDeviceId, otherSSKSignature, e))
        }

        return DeviceTrustResult.Success(DeviceTrustLevel(crossSigningVerified = true, locallyVerified = locallyTrusted))
    }

    fun checkDeviceTrust(myKeys: MXCrossSigningInfo?, otherKeys: MXCrossSigningInfo?, otherDevice: CryptoDeviceInfo): DeviceTrustResult {
        val locallyTrusted = otherDevice.trustLevel?.isLocallyVerified()
        myKeys ?: return legacyFallbackTrust(locallyTrusted, DeviceTrustResult.CrossSigningNotConfigured(myUserId))

        if (!myKeys.isTrusted()) return legacyFallbackTrust(locallyTrusted, DeviceTrustResult.KeysNotTrusted(myKeys))

        otherKeys ?: return legacyFallbackTrust(locallyTrusted, DeviceTrustResult.CrossSigningNotConfigured(otherDevice.userId))

        // TODO should we force verification ?
        if (!otherKeys.isTrusted()) return legacyFallbackTrust(locallyTrusted, DeviceTrustResult.KeysNotTrusted(otherKeys))

        // Check if the trust chain is valid
        /*
         *  ┏━━━━━━━━┓                             ┏━━━━━━━━┓
         *  ┃ ALICE  ┃                             ┃  BOB   ┃
         *  ┗━━━━━━━━┛                             ┗━━━━━━━━┛
         *   MSK                      ┌────────────▶MSK
         *                            │
         *     │                      │               │
         *     │    SSK               │               └──▶ SSK  ──────────────────┐
         *     │                      │                                           │
         *     │                      │                    USK                    │
         *     └──▶ USK   ────────────┘              (not visible by              │
         *                                                Alice)                  │
         *                                                                        ▼
         *                                                                ┌──────────────┐
         *                                                                │ BOB's Device │
         *                                                                └──────────────┘
         */

        val otherSSKSignature = otherDevice.signatures?.get(otherKeys.userId)?.get("ed25519:${otherKeys.selfSigningKey()?.unpaddedBase64PublicKey}")
                ?: return legacyFallbackTrust(
                        locallyTrusted,
                        DeviceTrustResult.MissingDeviceSignature(
                                otherDevice.deviceId, otherKeys.selfSigningKey()
                                ?.unpaddedBase64PublicKey
                                ?: ""
                        )
                )

        // Check  bob's device is signed by bob's SSK
        try {
            crossSigningOlm.olmUtility.verifyEd25519Signature(
                    otherSSKSignature,
                    otherKeys.selfSigningKey()?.unpaddedBase64PublicKey,
                    otherDevice.canonicalSignable()
            )
        } catch (e: Throwable) {
            return legacyFallbackTrust(locallyTrusted, DeviceTrustResult.InvalidDeviceSignature(otherDevice.deviceId, otherSSKSignature, e))
        }

        return DeviceTrustResult.Success(DeviceTrustLevel(crossSigningVerified = true, locallyVerified = locallyTrusted))
    }

    private fun legacyFallbackTrust(locallyTrusted: Boolean?, crossSignTrustFail: DeviceTrustResult): DeviceTrustResult {
        return if (locallyTrusted == true) {
            DeviceTrustResult.Success(DeviceTrustLevel(crossSigningVerified = false, locallyVerified = true))
        } else {
            crossSignTrustFail
        }
    }

    override fun onUsersDeviceUpdate(userIds: List<String>) {
        Timber.d("## CrossSigning - onUsersDeviceUpdate for users: ${userIds.logLimit()}")
        checkTrustAndAffectedRoomShields(userIds)
    }

    fun checkTrustAndAffectedRoomShields(userIds: List<String>) {
        Timber.d("## CrossSigning - checkTrustAndAffectedRoomShields for users: ${userIds.logLimit()}")
        val workerParams = UpdateTrustWorker.Params(
                sessionId = sessionId,
                filename = updateTrustWorkerDataRepository.createParam(userIds)
        )
        val workerData = WorkerParamsFactory.toData(workerParams)

        val workRequest = workManagerProvider.matrixOneTimeWorkRequestBuilder<UpdateTrustWorker>()
                .setInputData(workerData)
                .setBackoffCriteria(BackoffPolicy.LINEAR, WorkManagerProvider.BACKOFF_DELAY_MILLIS, TimeUnit.MILLISECONDS)
                .build()

        workManagerProvider.workManager
                .beginUniqueWork("TRUST_UPDATE_QUEUE", ExistingWorkPolicy.APPEND_OR_REPLACE, workRequest)
                .enqueue()
    }

    private fun setUserKeysAsTrusted(otherUserId: String, trusted: Boolean) {
        val currentTrust = cryptoStore.getCrossSigningInfo(otherUserId)?.isTrusted()
        cryptoStore.setUserKeysAsTrusted(otherUserId, trusted)
        // If it's me, recheck trust of all users and devices?
        val users = ArrayList<String>()
        if (otherUserId == myUserId && currentTrust != trusted) {
            // notify key requester
            outgoingKeyRequestManager.onSelfCrossSigningTrustChanged(trusted)
            cryptoStore.updateUsersTrust {
                users.add(it)
                checkUserTrust(it).isVerified()
            }

            users.forEach {
                cryptoStore.getUserDeviceList(it)?.forEach { device ->
                    val updatedTrust = checkDeviceTrust(it, device.deviceId, device.trustLevel?.isLocallyVerified() ?: false)
                    Timber.v("## CrossSigning - update trust for device ${device.deviceId} of user $otherUserId , verified=$updatedTrust")
                    cryptoStore.setDeviceTrust(it, device.deviceId, updatedTrust.isCrossSignedVerified(), updatedTrust.isLocallyVerified())
                }
            }
        }
    }
}
