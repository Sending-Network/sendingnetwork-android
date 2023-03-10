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
package org.sdn.android.sdk.internal.database.model

import io.realm.RealmObject
import org.sdn.android.sdk.api.session.pushers.PusherState

internal open class PusherEntity(
        var pushKey: String = "",
        var kind: String? = null,
        var appId: String = "",
        var appDisplayName: String? = null,
        var deviceDisplayName: String? = null,
        var profileTag: String? = null,
        var lang: String? = null,
        var data: PusherDataEntity? = null,
        var enabled: Boolean = true,
        var deviceId: String? = null,
) : RealmObject() {
    private var stateStr: String = PusherState.UNREGISTERED.name

    var state: PusherState
        get() {
            try {
                return PusherState.valueOf(stateStr)
            } catch (e: Exception) {
                // can this happen?
                return PusherState.UNREGISTERED
            }
        }
        set(value) {
            stateStr = value.name
        }

    companion object
}

internal fun PusherEntity.deleteOnCascade() {
    data?.deleteFromRealm()
    deleteFromRealm()
}
