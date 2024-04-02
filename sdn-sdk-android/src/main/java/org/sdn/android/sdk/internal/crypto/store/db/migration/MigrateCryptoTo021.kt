/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.sdn.android.sdk.internal.crypto.store.db.migration

import io.realm.DynamicRealm
import io.realm.FieldAttribute
import org.sdn.android.sdk.internal.crypto.store.db.model.CurrentGroupSessionEntityFields
import org.sdn.android.sdk.internal.util.database.RealmMigrator

/**
 * This migration create a new schema of CurrentGroupSessionEntity.
 */
internal class MigrateCryptoTo021(realm: DynamicRealm) : RealmMigrator(realm, 21) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.create("CurrentGroupSessionEntity")
            .addField(CurrentGroupSessionEntityFields.SESSION_ID, String::class.java)
            .addField(CurrentGroupSessionEntityFields.SENDER_KEY, String::class.java)
            .addField(CurrentGroupSessionEntityFields.ROOM_ID, String::class.java, FieldAttribute.PRIMARY_KEY)
            .addField(CurrentGroupSessionEntityFields.OLM_INBOUND_GROUP_SESSION_DATA, String::class.java)
            .addField(CurrentGroupSessionEntityFields.INBOUND_GROUP_SESSION_DATA_JSON, String::class.java)
            .addField(CurrentGroupSessionEntityFields.SERIALIZED_OLM_INBOUND_GROUP_SESSION, String::class.java)
            .addField(CurrentGroupSessionEntityFields.SHARED_HISTORY, Boolean::class.java)
            .addField(CurrentGroupSessionEntityFields.BACKED_UP, Boolean::class.java)
    }
}
