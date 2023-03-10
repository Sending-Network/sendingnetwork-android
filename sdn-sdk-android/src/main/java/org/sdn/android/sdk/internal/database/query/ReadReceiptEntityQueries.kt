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

package org.sdn.android.sdk.internal.database.query

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.sdn.android.sdk.api.session.room.read.ReadService
import org.sdn.android.sdk.internal.database.model.ReadReceiptEntity
import org.sdn.android.sdk.internal.database.model.ReadReceiptEntityFields

internal fun ReadReceiptEntity.Companion.where(realm: Realm, roomId: String, userId: String, threadId: String?): RealmQuery<ReadReceiptEntity> {
    return realm.where<ReadReceiptEntity>()
            .equalTo(ReadReceiptEntityFields.PRIMARY_KEY, buildPrimaryKey(roomId, userId, threadId))
}

internal fun ReadReceiptEntity.Companion.forMainTimelineWhere(realm: Realm, roomId: String, userId: String): RealmQuery<ReadReceiptEntity> {
    return realm.where<ReadReceiptEntity>()
            .equalTo(ReadReceiptEntityFields.PRIMARY_KEY, buildPrimaryKey(roomId, userId, ReadService.THREAD_ID_MAIN))
            .or()
            .equalTo(ReadReceiptEntityFields.PRIMARY_KEY, buildPrimaryKey(roomId, userId, null))
}

internal fun ReadReceiptEntity.Companion.whereUserId(realm: Realm, userId: String): RealmQuery<ReadReceiptEntity> {
    return realm.where<ReadReceiptEntity>()
            .equalTo(ReadReceiptEntityFields.USER_ID, userId)
}

internal fun ReadReceiptEntity.Companion.whereRoomId(realm: Realm, roomId: String): RealmQuery<ReadReceiptEntity> {
    return realm.where<ReadReceiptEntity>()
            .equalTo(ReadReceiptEntityFields.ROOM_ID, roomId)
}

internal fun ReadReceiptEntity.Companion.createUnmanaged(
        roomId: String,
        eventId: String,
        userId: String,
        threadId: String?,
        originServerTs: Double
): ReadReceiptEntity {
    return ReadReceiptEntity().apply {
        this.primaryKey = buildPrimaryKey(roomId, userId, threadId)
        this.eventId = eventId
        this.roomId = roomId
        this.userId = userId
        this.threadId = threadId
        this.originServerTs = originServerTs
    }
}

internal fun ReadReceiptEntity.Companion.getOrCreate(realm: Realm, roomId: String, userId: String, threadId: String?): ReadReceiptEntity {
    return ReadReceiptEntity.where(realm, roomId, userId, threadId).findFirst()
            ?: realm.createObject<ReadReceiptEntity>(buildPrimaryKey(roomId, userId, threadId))
                    .apply {
                        this.roomId = roomId
                        this.userId = userId
                        this.threadId = threadId
                    }
}

private fun buildPrimaryKey(roomId: String, userId: String, threadId: String?): String {
    return if (threadId == null) {
        "${roomId}_${userId}"
    } else {
        "${roomId}_${userId}_${threadId}"
    }
}
