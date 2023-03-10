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

package org.sdn.android.sdk.internal.session.filter

import org.sdn.android.sdk.api.session.sync.FilterService
import org.sdn.android.sdk.api.session.sync.filter.SyncFilterBuilder
import org.sdn.android.sdk.internal.session.node.NodeCapabilitiesDataSource
import javax.inject.Inject

internal class DefaultFilterService @Inject constructor(
    private val saveFilterTask: SaveFilterTask,
    private val filterRepository: FilterRepository,
    private val nodeCapabilitiesDataSource: NodeCapabilitiesDataSource,
) : FilterService {

    // TODO Pass a list of support events instead
    override suspend fun setSyncFilter(filterBuilder: SyncFilterBuilder) {
        filterRepository.storeFilterParams(filterBuilder.extractParams())

        // don't upload/store filter until homeserver capabilities are fetched
        nodeCapabilitiesDataSource.getNodeCapabilities()?.let { homeServerCapabilities ->
            saveFilterTask.execute(
                    SaveFilterTask.Params(
                            filter = filterBuilder.build(homeServerCapabilities)
                    )
            )
        }
    }
}
