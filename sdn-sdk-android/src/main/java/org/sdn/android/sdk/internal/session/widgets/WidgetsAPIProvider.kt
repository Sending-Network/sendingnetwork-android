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

package org.sdn.android.sdk.internal.session.widgets

import dagger.Lazy
import okhttp3.OkHttpClient
import org.sdn.android.sdk.internal.di.Unauthenticated
import org.sdn.android.sdk.internal.network.RetrofitFactory
import org.sdn.android.sdk.internal.session.SessionScope
import javax.inject.Inject

@SessionScope
internal class WidgetsAPIProvider @Inject constructor(
        @Unauthenticated private val okHttpClient: Lazy<OkHttpClient>,
        private val retrofitFactory: RetrofitFactory
) {

    // Map to keep one WidgetAPI instance by serverUrl
    private val widgetsAPIs = mutableMapOf<String, WidgetsAPI>()

    fun get(serverUrl: String): WidgetsAPI {
        return widgetsAPIs.getOrPut(serverUrl) {
            retrofitFactory.create(okHttpClient, serverUrl).create(WidgetsAPI::class.java)
        }
    }
}
