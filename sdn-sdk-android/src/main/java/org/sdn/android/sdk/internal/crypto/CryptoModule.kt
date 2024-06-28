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

package org.sdn.android.sdk.internal.crypto

import dagger.Binds
import dagger.Module
import dagger.Provides
import io.realm.RealmConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.sdn.android.sdk.api.session.crypto.CryptoService
import org.sdn.android.sdk.api.session.crypto.crosssigning.CrossSigningService
import org.sdn.android.sdk.internal.crypto.api.CryptoApi
import org.sdn.android.sdk.internal.crypto.crosssigning.ComputeTrustTask
import org.sdn.android.sdk.internal.crypto.crosssigning.DefaultComputeTrustTask
import org.sdn.android.sdk.internal.crypto.crosssigning.DefaultCrossSigningService
import org.sdn.android.sdk.internal.crypto.keysbackup.api.RoomKeysApi
import org.sdn.android.sdk.internal.crypto.keysbackup.tasks.CreateKeysBackupVersionTask
import org.sdn.android.sdk.internal.crypto.keysbackup.tasks.DefaultCreateKeysBackupVersionTask
import org.sdn.android.sdk.internal.crypto.keysbackup.tasks.DefaultDeleteBackupTask
import org.sdn.android.sdk.internal.crypto.keysbackup.tasks.DefaultDeleteRoomSessionDataTask
import org.sdn.android.sdk.internal.crypto.keysbackup.tasks.DefaultDeleteRoomSessionsDataTask
import org.sdn.android.sdk.internal.crypto.keysbackup.tasks.DefaultDeleteSessionsDataTask
import org.sdn.android.sdk.internal.crypto.keysbackup.tasks.DefaultGetKeysBackupLastVersionTask
import org.sdn.android.sdk.internal.crypto.keysbackup.tasks.DefaultGetKeysBackupVersionTask
import org.sdn.android.sdk.internal.crypto.keysbackup.tasks.DefaultGetRoomSessionDataTask
import org.sdn.android.sdk.internal.crypto.keysbackup.tasks.DefaultGetRoomSessionsDataTask
import org.sdn.android.sdk.internal.crypto.keysbackup.tasks.DefaultGetSessionsDataTask
import org.sdn.android.sdk.internal.crypto.keysbackup.tasks.DefaultStoreRoomSessionDataTask
import org.sdn.android.sdk.internal.crypto.keysbackup.tasks.DefaultStoreRoomSessionsDataTask
import org.sdn.android.sdk.internal.crypto.keysbackup.tasks.DefaultStoreSessionsDataTask
import org.sdn.android.sdk.internal.crypto.keysbackup.tasks.DefaultUpdateKeysBackupVersionTask
import org.sdn.android.sdk.internal.crypto.keysbackup.tasks.DeleteBackupTask
import org.sdn.android.sdk.internal.crypto.keysbackup.tasks.DeleteRoomSessionDataTask
import org.sdn.android.sdk.internal.crypto.keysbackup.tasks.DeleteRoomSessionsDataTask
import org.sdn.android.sdk.internal.crypto.keysbackup.tasks.DeleteSessionsDataTask
import org.sdn.android.sdk.internal.crypto.keysbackup.tasks.GetKeysBackupLastVersionTask
import org.sdn.android.sdk.internal.crypto.keysbackup.tasks.GetKeysBackupVersionTask
import org.sdn.android.sdk.internal.crypto.keysbackup.tasks.GetRoomSessionDataTask
import org.sdn.android.sdk.internal.crypto.keysbackup.tasks.GetRoomSessionsDataTask
import org.sdn.android.sdk.internal.crypto.keysbackup.tasks.GetSessionsDataTask
import org.sdn.android.sdk.internal.crypto.keysbackup.tasks.StoreRoomSessionDataTask
import org.sdn.android.sdk.internal.crypto.keysbackup.tasks.StoreRoomSessionsDataTask
import org.sdn.android.sdk.internal.crypto.keysbackup.tasks.StoreSessionsDataTask
import org.sdn.android.sdk.internal.crypto.keysbackup.tasks.UpdateKeysBackupVersionTask
import org.sdn.android.sdk.internal.crypto.store.IMXCryptoStore
import org.sdn.android.sdk.internal.crypto.store.db.RealmCryptoStore
import org.sdn.android.sdk.internal.crypto.store.db.RealmCryptoStoreMigration
import org.sdn.android.sdk.internal.crypto.store.db.RealmCryptoStoreModule
import org.sdn.android.sdk.internal.crypto.tasks.ClaimOneTimeKeysForUsersDeviceTask
import org.sdn.android.sdk.internal.crypto.tasks.DefaultClaimOneTimeKeysForUsersDevice
import org.sdn.android.sdk.internal.crypto.tasks.DefaultDeleteDeviceTask
import org.sdn.android.sdk.internal.crypto.tasks.DefaultDownloadKeysForUsers
import org.sdn.android.sdk.internal.crypto.tasks.DefaultEncryptEventTask
import org.sdn.android.sdk.internal.crypto.tasks.DefaultGetDeviceInfoTask
import org.sdn.android.sdk.internal.crypto.tasks.DefaultGetDevicesTask
import org.sdn.android.sdk.internal.crypto.tasks.DefaultGetSessionMapTask
import org.sdn.android.sdk.internal.crypto.tasks.DefaultInitializeCrossSigningTask
import org.sdn.android.sdk.internal.crypto.tasks.DefaultPullSessionKeysTask
import org.sdn.android.sdk.internal.crypto.tasks.DefaultPutSessionMapTask
import org.sdn.android.sdk.internal.crypto.tasks.DefaultSendEventTask
import org.sdn.android.sdk.internal.crypto.tasks.DefaultSendSimpleEventTask
import org.sdn.android.sdk.internal.crypto.tasks.DefaultSendToDeviceTask
import org.sdn.android.sdk.internal.crypto.tasks.DefaultSendVerificationMessageTask
import org.sdn.android.sdk.internal.crypto.tasks.DefaultSetDeviceNameTask
import org.sdn.android.sdk.internal.crypto.tasks.DefaultUploadKeysTask
import org.sdn.android.sdk.internal.crypto.tasks.DefaultUploadSignaturesTask
import org.sdn.android.sdk.internal.crypto.tasks.DefaultUploadSigningKeysTask
import org.sdn.android.sdk.internal.crypto.tasks.DeleteDeviceTask
import org.sdn.android.sdk.internal.crypto.tasks.DownloadKeysForUsersTask
import org.sdn.android.sdk.internal.crypto.tasks.EncryptEventTask
import org.sdn.android.sdk.internal.crypto.tasks.GetDeviceInfoTask
import org.sdn.android.sdk.internal.crypto.tasks.GetDevicesTask
import org.sdn.android.sdk.internal.crypto.tasks.GetSessionMapTask
import org.sdn.android.sdk.internal.crypto.tasks.InitializeCrossSigningTask
import org.sdn.android.sdk.internal.crypto.tasks.PullSessionKeysTask
import org.sdn.android.sdk.internal.crypto.tasks.PutSessionMapTask
import org.sdn.android.sdk.internal.crypto.tasks.SendEventTask
import org.sdn.android.sdk.internal.crypto.tasks.SendSimpleEventTask
import org.sdn.android.sdk.internal.crypto.tasks.SendToDeviceTask
import org.sdn.android.sdk.internal.crypto.tasks.SendVerificationMessageTask
import org.sdn.android.sdk.internal.crypto.tasks.SetDeviceNameTask
import org.sdn.android.sdk.internal.crypto.tasks.UploadKeysTask
import org.sdn.android.sdk.internal.crypto.tasks.UploadSignaturesTask
import org.sdn.android.sdk.internal.crypto.tasks.UploadSigningKeysTask
import org.sdn.android.sdk.internal.database.RealmKeysUtils
import org.sdn.android.sdk.internal.di.CryptoDatabase
import org.sdn.android.sdk.internal.di.SessionFilesDirectory
import org.sdn.android.sdk.internal.di.UserMd5
import org.sdn.android.sdk.internal.session.SessionScope
import org.sdn.android.sdk.internal.session.cache.ClearCacheTask
import org.sdn.android.sdk.internal.session.cache.RealmClearCacheTask
import retrofit2.Retrofit
import java.io.File

@Module
internal abstract class CryptoModule {

    @Module
    companion object {
        internal fun getKeyAlias(userMd5: String) = "crypto_module_$userMd5"

        @JvmStatic
        @Provides
        @CryptoDatabase
        @SessionScope
        fun providesRealmConfiguration(
                @SessionFilesDirectory directory: File,
                @UserMd5 userMd5: String,
                realmKeysUtils: RealmKeysUtils,
                realmCryptoStoreMigration: RealmCryptoStoreMigration
        ): RealmConfiguration {
            return RealmConfiguration.Builder()
                    .directory(directory)
                    .apply {
                        realmKeysUtils.configureEncryption(this, getKeyAlias(userMd5))
                    }
                    .name("crypto_store.realm")
                    .modules(RealmCryptoStoreModule())
                    .allowWritesOnUiThread(true)
                    .schemaVersion(realmCryptoStoreMigration.schemaVersion)
                    .migration(realmCryptoStoreMigration)
                    .build()
        }

        @JvmStatic
        @Provides
        @SessionScope
        fun providesCryptoCoroutineScope(): CoroutineScope {
            return CoroutineScope(SupervisorJob())
        }

        @JvmStatic
        @Provides
        @CryptoDatabase
        fun providesClearCacheTask(@CryptoDatabase realmConfiguration: RealmConfiguration): ClearCacheTask {
            return RealmClearCacheTask(realmConfiguration)
        }

        @JvmStatic
        @Provides
        @SessionScope
        fun providesCryptoAPI(retrofit: Retrofit): CryptoApi {
            return retrofit.create(CryptoApi::class.java)
        }

        @JvmStatic
        @Provides
        @SessionScope
        fun providesRoomKeysAPI(retrofit: Retrofit): RoomKeysApi {
            return retrofit.create(RoomKeysApi::class.java)
        }
    }

    @Binds
    abstract fun bindCryptoService(service: DefaultCryptoService): CryptoService

    @Binds
    abstract fun bindDeleteDeviceTask(task: DefaultDeleteDeviceTask): DeleteDeviceTask

    @Binds
    abstract fun bindGetDevicesTask(task: DefaultGetDevicesTask): GetDevicesTask

    @Binds
    abstract fun bindGetDeviceInfoTask(task: DefaultGetDeviceInfoTask): GetDeviceInfoTask

    @Binds
    abstract fun bindSetDeviceNameTask(task: DefaultSetDeviceNameTask): SetDeviceNameTask

    @Binds
    abstract fun bindUploadKeysTask(task: DefaultUploadKeysTask): UploadKeysTask

    @Binds
    abstract fun bindUploadSigningKeysTask(task: DefaultUploadSigningKeysTask): UploadSigningKeysTask

    @Binds
    abstract fun bindUploadSignaturesTask(task: DefaultUploadSignaturesTask): UploadSignaturesTask

    @Binds
    abstract fun bindDownloadKeysForUsersTask(task: DefaultDownloadKeysForUsers): DownloadKeysForUsersTask

    @Binds
    abstract fun bindCreateKeysBackupVersionTask(task: DefaultCreateKeysBackupVersionTask): CreateKeysBackupVersionTask

    @Binds
    abstract fun bindDeleteBackupTask(task: DefaultDeleteBackupTask): DeleteBackupTask

    @Binds
    abstract fun bindDeleteRoomSessionDataTask(task: DefaultDeleteRoomSessionDataTask): DeleteRoomSessionDataTask

    @Binds
    abstract fun bindDeleteRoomSessionsDataTask(task: DefaultDeleteRoomSessionsDataTask): DeleteRoomSessionsDataTask

    @Binds
    abstract fun bindDeleteSessionsDataTask(task: DefaultDeleteSessionsDataTask): DeleteSessionsDataTask

    @Binds
    abstract fun bindGetKeysBackupLastVersionTask(task: DefaultGetKeysBackupLastVersionTask): GetKeysBackupLastVersionTask

    @Binds
    abstract fun bindGetKeysBackupVersionTask(task: DefaultGetKeysBackupVersionTask): GetKeysBackupVersionTask

    @Binds
    abstract fun bindGetRoomSessionDataTask(task: DefaultGetRoomSessionDataTask): GetRoomSessionDataTask

    @Binds
    abstract fun bindGetRoomSessionsDataTask(task: DefaultGetRoomSessionsDataTask): GetRoomSessionsDataTask

    @Binds
    abstract fun bindGetSessionsDataTask(task: DefaultGetSessionsDataTask): GetSessionsDataTask

    @Binds
    abstract fun bindStoreRoomSessionDataTask(task: DefaultStoreRoomSessionDataTask): StoreRoomSessionDataTask

    @Binds
    abstract fun bindStoreRoomSessionsDataTask(task: DefaultStoreRoomSessionsDataTask): StoreRoomSessionsDataTask

    @Binds
    abstract fun bindStoreSessionsDataTask(task: DefaultStoreSessionsDataTask): StoreSessionsDataTask

    @Binds
    abstract fun bindUpdateKeysBackupVersionTask(task: DefaultUpdateKeysBackupVersionTask): UpdateKeysBackupVersionTask

    @Binds
    abstract fun bindSendToDeviceTask(task: DefaultSendToDeviceTask): SendToDeviceTask

    @Binds
    abstract fun bindSendSimpleEventTask(task: DefaultSendSimpleEventTask): SendSimpleEventTask

    @Binds
    abstract fun bindPullRoomKeyTask(task: DefaultPullSessionKeysTask): PullSessionKeysTask

    @Binds
    abstract fun bindGetSessionMapTask(task: DefaultGetSessionMapTask): GetSessionMapTask

    @Binds
    abstract fun bindPutSessionMapTask(task: DefaultPutSessionMapTask): PutSessionMapTask

    @Binds
    abstract fun bindEncryptEventTask(task: DefaultEncryptEventTask): EncryptEventTask

    @Binds
    abstract fun bindSendVerificationMessageTask(task: DefaultSendVerificationMessageTask): SendVerificationMessageTask

    @Binds
    abstract fun bindClaimOneTimeKeysForUsersDeviceTask(task: DefaultClaimOneTimeKeysForUsersDevice): ClaimOneTimeKeysForUsersDeviceTask

    @Binds
    abstract fun bindCrossSigningService(service: DefaultCrossSigningService): CrossSigningService

    @Binds
    abstract fun bindCryptoStore(store: RealmCryptoStore): IMXCryptoStore

    @Binds
    abstract fun bindComputeShieldTrustTask(task: DefaultComputeTrustTask): ComputeTrustTask

    @Binds
    abstract fun bindInitializeCrossSigningTask(task: DefaultInitializeCrossSigningTask): InitializeCrossSigningTask

    @Binds
    abstract fun bindSendEventTask(task: DefaultSendEventTask): SendEventTask
}
