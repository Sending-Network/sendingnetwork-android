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
import io.realm.RealmList
import org.sdn.android.sdk.internal.database.model.RoomSummaryEntityFields
import org.sdn.android.sdk.internal.util.database.RealmMigrator

internal class MigrateSessionTo035(realm: DynamicRealm) : RealmMigrator(realm, 35) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.get("RoomSummaryEntity")
                ?.addRealmListField(RoomSummaryEntityFields.DIRECT_PARENT_NAMES.`$`, String::class.java)
                ?.transform { it.setList(RoomSummaryEntityFields.DIRECT_PARENT_NAMES.`$`, RealmList("")) }
    }
}
