/*
 * Copyright (c) 2020 New Vector Ltd
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

package org.sdn.android.sdk.sample

import android.app.Application
import android.content.Context
import org.sdn.android.sdk.api.SDNClient
import org.sdn.android.sdk.api.SDNConfiguration
import org.sdn.android.sdk.sample.util.RoomDisplayNameFallbackProviderImpl
import timber.log.Timber

class SampleApp : Application() {

    private lateinit var sdnClient: SDNClient

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        // You should first create a Matrix instance before using it
        createSDNClient()
        // You can then grab the authentication service and search for a known session
        val lastSession = sdnClient.authenticationService().getLastAuthenticatedSession()
        if (lastSession != null) {
            SessionHolder.currentSession = lastSession
            // Don't forget to open the session and start syncing.

            lastSession.open()
            lastSession.syncService().startSync(true)
        }
    }

    private fun createSDNClient() {
        sdnClient = SDNClient(
            context = this,
            sdnConfiguration = SDNConfiguration(
                roomDisplayNameFallbackProvider = RoomDisplayNameFallbackProviderImpl()
            )
        )
    }

    companion object {
        fun getSDNClient(context: Context): SDNClient {
            return (context.applicationContext as SampleApp).sdnClient
        }
    }
}
