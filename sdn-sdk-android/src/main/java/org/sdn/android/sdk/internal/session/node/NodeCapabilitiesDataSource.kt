/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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

package org.sdn.android.sdk.internal.session.node

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import io.realm.kotlin.where
import org.sdn.android.sdk.api.session.node.NodeCapabilities
import org.sdn.android.sdk.api.util.Optional
import org.sdn.android.sdk.api.util.toOptional
import org.sdn.android.sdk.internal.database.mapper.NodeCapabilitiesMapper
import org.sdn.android.sdk.internal.database.model.HomeServerCapabilitiesEntity
import org.sdn.android.sdk.internal.database.query.get
import org.sdn.android.sdk.internal.di.SessionDatabase
import javax.inject.Inject

internal class NodeCapabilitiesDataSource @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy,
) {
    fun getNodeCapabilities(): NodeCapabilities? {
        return Realm.getInstance(monarchy.realmConfiguration).use { realm ->
            HomeServerCapabilitiesEntity.get(realm)?.let {
                NodeCapabilitiesMapper.map(it)
            }
        }
    }

    fun getNodeCapabilitiesLive(): LiveData<Optional<NodeCapabilities>> {
        val liveData = monarchy.findAllMappedWithChanges(
                { realm: Realm -> realm.where<HomeServerCapabilitiesEntity>() },
                { NodeCapabilitiesMapper.map(it) }
        )
        return Transformations.map(liveData) {
            it.firstOrNull().toOptional()
        }
    }
}
