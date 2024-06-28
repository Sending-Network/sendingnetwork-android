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

package org.sdn.android.sdk.internal.crypto.store.db.query

import io.realm.Realm
import io.realm.RealmResults
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.sdn.android.sdk.internal.crypto.store.db.model.SharedSessionEntity
import org.sdn.android.sdk.internal.crypto.store.db.model.SharedSessionEntityFields

internal fun SharedSessionEntity.Companion.get(
        realm: Realm,
        roomId: String?,
        sessionId: String,
        userId: String,
        deviceId: String,
        deviceIdentityKey: String?
): SharedSessionEntity? {
    return realm.where<SharedSessionEntity>()
            .equalTo(SharedSessionEntityFields.ROOM_ID, roomId)
            .equalTo(SharedSessionEntityFields.SESSION_ID, sessionId)
            .equalTo(SharedSessionEntityFields.USER_ID, userId)
            .equalTo(SharedSessionEntityFields.DEVICE_ID, deviceId)
            .equalTo(SharedSessionEntityFields.DEVICE_IDENTITY_KEY, deviceIdentityKey)
            .findFirst()
}

internal fun SharedSessionEntity.Companion.get(realm: Realm, roomId: String?, sessionId: String): RealmResults<SharedSessionEntity> {
    return realm.where<SharedSessionEntity>()
            .equalTo(SharedSessionEntityFields.ROOM_ID, roomId)
            .equalTo(SharedSessionEntityFields.SESSION_ID, sessionId)
            .findAll()
}

internal fun SharedSessionEntity.Companion.create(
        realm: Realm,
        roomId: String?,
        sessionId: String,
        userId: String,
        deviceId: String,
        deviceIdentityKey: String,
        chainIndex: Int,
        lastUpdate: Long?,
        directShare: Boolean?
): SharedSessionEntity {
    return realm.createObject<SharedSessionEntity>().apply {
        this.roomId = roomId
        this.sessionId = sessionId
        this.userId = userId
        this.deviceId = deviceId
        this.deviceIdentityKey = deviceIdentityKey
        this.chainIndex = chainIndex
        this.lastUpdate = lastUpdate
        this.directShare = directShare
    }
}
