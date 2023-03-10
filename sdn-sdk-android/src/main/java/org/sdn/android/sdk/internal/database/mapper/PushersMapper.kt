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
package org.sdn.android.sdk.internal.database.mapper

import org.sdn.android.sdk.api.session.pushers.Pusher
import org.sdn.android.sdk.api.session.pushers.PusherData
import org.sdn.android.sdk.internal.database.model.PusherDataEntity
import org.sdn.android.sdk.internal.database.model.PusherEntity
import org.sdn.android.sdk.internal.session.pushers.JsonPusher

internal object PushersMapper {

    fun map(pushEntity: PusherEntity): Pusher {
        return Pusher(
                pushKey = pushEntity.pushKey,
                kind = pushEntity.kind ?: "",
                appId = pushEntity.appId,
                appDisplayName = pushEntity.appDisplayName,
                deviceDisplayName = pushEntity.deviceDisplayName,
                profileTag = pushEntity.profileTag,
                lang = pushEntity.lang,
                data = PusherData(pushEntity.data?.url, pushEntity.data?.format),
                enabled = pushEntity.enabled,
                deviceId = pushEntity.deviceId,
                state = pushEntity.state,
        )
    }

    fun map(pusher: JsonPusher): PusherEntity {
        return PusherEntity(
                pushKey = pusher.pushKey,
                kind = pusher.kind,
                appId = pusher.appId,
                appDisplayName = pusher.appDisplayName,
                deviceDisplayName = pusher.deviceDisplayName,
                profileTag = pusher.profileTag,
                lang = pusher.lang,
                data = PusherDataEntity(pusher.data?.url, pusher.data?.format),
                enabled = pusher.enabled,
                deviceId = pusher.deviceId,
        )
    }
}

internal fun PusherEntity.asDomain(): Pusher {
    return PushersMapper.map(this)
}

internal fun JsonPusher.toEntity(): PusherEntity {
    return PushersMapper.map(this)
}
