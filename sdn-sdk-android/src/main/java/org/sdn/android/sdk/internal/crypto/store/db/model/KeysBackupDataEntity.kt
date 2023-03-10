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

package org.sdn.android.sdk.internal.crypto.store.db.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

internal open class KeysBackupDataEntity(
        // Primary key to update this object. There is only one object, so it's a constant, please do not set it
        @PrimaryKey
        var primaryKey: Int = 0,
        // The last known hash of the backed up keys on the server
        var backupLastServerHash: String? = null,
        // The last known number of backed up keys on the server
        var backupLastServerNumberOfKeys: Int? = null
) : RealmObject()
