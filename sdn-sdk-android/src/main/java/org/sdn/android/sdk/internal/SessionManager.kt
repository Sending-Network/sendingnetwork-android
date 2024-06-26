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

package org.sdn.android.sdk.internal

import org.sdn.android.sdk.api.auth.data.SessionParams
import org.sdn.android.sdk.api.auth.data.sessionId
import org.sdn.android.sdk.api.session.Session
import org.sdn.android.sdk.internal.auth.SessionParamsStore
import org.sdn.android.sdk.internal.di.SDNComponent
import org.sdn.android.sdk.internal.di.SDNScope
import org.sdn.android.sdk.internal.session.DaggerSessionComponent
import org.sdn.android.sdk.internal.session.SessionComponent
import javax.inject.Inject

@SDNScope
internal class SessionManager @Inject constructor(
    private val sdnComponent: SDNComponent,
    private val sessionParamsStore: SessionParamsStore
) {

    // SessionId -> SessionComponent
    private val sessionComponents = HashMap<String, SessionComponent>()

    fun getSessionComponent(sessionId: String): SessionComponent? {
        val sessionParams = sessionParamsStore.get(sessionId) ?: return null
        return getOrCreateSessionComponent(sessionParams)
    }

    fun getLastSession(): Session? {
        val sessionParams = sessionParamsStore.getLast()
        return sessionParams?.let {
            getOrCreateSession(it)
        }
    }

    fun getSession(sessionId: String): Session? {
        val sessionParams = sessionParamsStore.get(sessionId)
        return sessionParams?.let {
            getOrCreateSession(it)
        }
    }

    fun getOrCreateSession(sessionParams: SessionParams): Session {
        return getOrCreateSessionComponent(sessionParams).session()
    }

    fun releaseSession(sessionId: String) {
        if (sessionComponents.containsKey(sessionId).not()) {
            throw RuntimeException("You don't have a session for id $sessionId")
        }
        sessionComponents.remove(sessionId)?.also {
            it.session().close()
        }
    }

    fun stopSession(sessionId: String) {
        val sessionComponent = sessionComponents[sessionId] ?: throw RuntimeException("You don't have a session for id $sessionId")
        sessionComponent.session().syncService().stopSync()
    }

    fun getOrCreateSessionComponent(sessionParams: SessionParams): SessionComponent {
        return sessionComponents.getOrPut(sessionParams.credentials.sessionId()) {
            DaggerSessionComponent
                    .factory()
                    .create(sdnComponent, sessionParams)
        }
    }
}
