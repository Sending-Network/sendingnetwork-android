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

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import org.sdn.android.sdk.api.session.crypto.crosssigning.KeyUsage
import org.sdn.android.sdk.internal.extensions.clearWith

internal open class CrossSigningInfoEntity(
        @PrimaryKey
        var userId: String? = null,
        var wasUserVerifiedOnce: Boolean = false,
        var crossSigningKeys: RealmList<KeyInfoEntity> = RealmList()
) : RealmObject() {

    companion object

    fun getMasterKey() = crossSigningKeys.firstOrNull { it.usages.contains(KeyUsage.MASTER.value) }

    fun setMasterKey(info: KeyInfoEntity?) {
        crossSigningKeys
                .filter { it.usages.contains(KeyUsage.MASTER.value) }
                .forEach { crossSigningKeys.remove(it) }
        info?.let { crossSigningKeys.add(it) }
    }

    fun getSelfSignedKey() = crossSigningKeys.firstOrNull { it.usages.contains(KeyUsage.SELF_SIGNING.value) }

    fun setSelfSignedKey(info: KeyInfoEntity?) {
        crossSigningKeys
                .filter { it.usages.contains(KeyUsage.SELF_SIGNING.value) }
                .forEach { crossSigningKeys.remove(it) }
        info?.let { crossSigningKeys.add(it) }
    }

    fun getUserSigningKey() = crossSigningKeys.firstOrNull { it.usages.contains(KeyUsage.USER_SIGNING.value) }

    fun setUserSignedKey(info: KeyInfoEntity?) {
        crossSigningKeys
                .filter { it.usages.contains(KeyUsage.USER_SIGNING.value) }
                .forEach { crossSigningKeys.remove(it) }
        info?.let { crossSigningKeys.add(it) }
    }
}

internal fun CrossSigningInfoEntity.deleteOnCascade() {
    crossSigningKeys.clearWith { it.deleteOnCascade() }
    deleteFromRealm()
}
