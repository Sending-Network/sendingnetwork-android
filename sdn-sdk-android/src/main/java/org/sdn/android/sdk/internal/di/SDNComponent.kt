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

package org.sdn.android.sdk.internal.di

import android.content.Context
import android.content.res.Resources
import com.squareup.moshi.Moshi
import dagger.BindsInstance
import dagger.Component
import okhttp3.OkHttpClient
import org.sdn.android.sdk.api.SDNClient
import org.sdn.android.sdk.api.SDNConfiguration
import org.sdn.android.sdk.api.SDNCoroutineDispatchers
import org.sdn.android.sdk.api.auth.AuthenticationService
import org.sdn.android.sdk.api.auth.NodeHistoryService
import org.sdn.android.sdk.api.raw.RawService
import org.sdn.android.sdk.api.securestorage.SecureStorageModule
import org.sdn.android.sdk.api.securestorage.SecureStorageService
import org.sdn.android.sdk.api.settings.LightweightSettingsStorage
import org.sdn.android.sdk.internal.SessionManager
import org.sdn.android.sdk.internal.auth.AuthModule
import org.sdn.android.sdk.internal.auth.SessionParamsStore
import org.sdn.android.sdk.internal.debug.DebugModule
import org.sdn.android.sdk.internal.raw.RawModule
import org.sdn.android.sdk.internal.session.MockHttpInterceptor
import org.sdn.android.sdk.internal.session.TestInterceptor
import org.sdn.android.sdk.internal.settings.SettingsModule
import org.sdn.android.sdk.internal.task.TaskExecutor
import org.sdn.android.sdk.internal.util.BackgroundDetectionObserver
import org.sdn.android.sdk.internal.util.system.SystemModule
import org.sdn.android.sdk.internal.worker.SDNWorkerFactory
import org.matrix.olm.OlmManager
import java.io.File

@Component(
        modules = [
            SDNModule::class,
            NetworkModule::class,
            AuthModule::class,
            RawModule::class,
            DebugModule::class,
            SettingsModule::class,
            SystemModule::class,
            NoOpTestModule::class,
            SecureStorageModule::class,
        ]
)
@SDNScope
internal interface SDNComponent {

    fun matrixCoroutineDispatchers(): SDNCoroutineDispatchers

    fun moshi(): Moshi

    @Unauthenticated
    fun okHttpClient(): OkHttpClient

    @MockHttpInterceptor
    fun testInterceptor(): TestInterceptor?

    fun authenticationService(): AuthenticationService

    fun rawService(): RawService

    fun lightweightSettingsStorage(): LightweightSettingsStorage

    fun homeServerHistoryService(): NodeHistoryService

    fun context(): Context

    fun sdnConfiguration(): SDNConfiguration

    fun resources(): Resources

    @CacheDirectory
    fun cacheDir(): File

    fun olmManager(): OlmManager

    fun taskExecutor(): TaskExecutor

    fun sessionParamsStore(): SessionParamsStore

    fun backgroundDetectionObserver(): BackgroundDetectionObserver

    fun sessionManager(): SessionManager

    fun secureStorageService(): SecureStorageService

    fun matrixWorkerFactory(): SDNWorkerFactory

    fun inject(sdnClient: SDNClient)

    @Component.Factory
    interface Factory {
        fun create(
                @BindsInstance context: Context,
                @BindsInstance sdnConfiguration: SDNConfiguration
        ): SDNComponent
    }
}
