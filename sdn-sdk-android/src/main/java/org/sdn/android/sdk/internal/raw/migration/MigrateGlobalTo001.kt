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

package org.sdn.android.sdk.internal.raw.migration

import io.realm.DynamicRealm
import org.sdn.android.sdk.internal.database.model.KnownServerUrlEntityFields
import org.sdn.android.sdk.internal.util.database.RealmMigrator

internal class MigrateGlobalTo001(realm: DynamicRealm) : RealmMigrator(realm, 1) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.create("KnownServerUrlEntity")
                .addField(KnownServerUrlEntityFields.URL, String::class.java)
                .addPrimaryKey(KnownServerUrlEntityFields.URL)
                .setRequired(KnownServerUrlEntityFields.URL, true)
    }
}
