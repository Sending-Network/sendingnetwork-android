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

package org.sdn.android.sdk.internal.database.migration

import io.realm.DynamicRealm
import org.sdn.android.sdk.internal.database.model.HomeServerCapabilitiesEntityFields
import org.sdn.android.sdk.internal.extensions.forceRefreshOfHomeServerCapabilities
import org.sdn.android.sdk.internal.util.database.RealmMigrator

internal class MigrateSessionTo016(realm: DynamicRealm) : RealmMigrator(realm, 16) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.get("HomeServerCapabilitiesEntity")
                ?.addField(HomeServerCapabilitiesEntityFields.ROOM_VERSIONS_JSON, String::class.java)
                ?.forceRefreshOfHomeServerCapabilities()
    }
}
