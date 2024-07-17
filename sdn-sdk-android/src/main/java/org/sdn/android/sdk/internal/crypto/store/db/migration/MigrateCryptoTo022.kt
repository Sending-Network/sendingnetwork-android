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
import org.sdn.android.sdk.internal.crypto.store.db.model.MyDeviceLastSeenInfoEntityFields
import org.sdn.android.sdk.internal.crypto.store.db.model.SharedSessionEntityFields
import org.sdn.android.sdk.internal.util.database.RealmMigrator

/**
 * This migration adds new fields into SharedSessionEntity
 */
internal class MigrateCryptoTo022(realm: DynamicRealm) : RealmMigrator(realm, 22) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.get("SharedSessionEntity")
                ?.addField(SharedSessionEntityFields.LAST_UPDATE, Long::class.java)
                ?.setNullable(SharedSessionEntityFields.LAST_UPDATE, true)
                ?.addField(SharedSessionEntityFields.DIRECT_SHARE, Boolean::class.java)
                ?.setNullable(SharedSessionEntityFields.DIRECT_SHARE, true)
    }
}
