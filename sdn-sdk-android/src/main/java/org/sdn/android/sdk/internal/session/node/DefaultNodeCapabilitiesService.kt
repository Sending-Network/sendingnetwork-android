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

package org.sdn.android.sdk.internal.session.node

import androidx.lifecycle.LiveData
import org.sdn.android.sdk.api.session.node.NodeCapabilities
import org.sdn.android.sdk.api.session.node.NodeCapabilitiesService
import org.sdn.android.sdk.api.util.Optional
import javax.inject.Inject

internal class DefaultNodeCapabilitiesService @Inject constructor(
    private val nodeCapabilitiesDataSource: NodeCapabilitiesDataSource,
    private val getNodeCapabilitiesTask: GetNodeCapabilitiesTask
) : NodeCapabilitiesService {

    override suspend fun refreshNodeCapabilities() {
        getNodeCapabilitiesTask.execute(GetNodeCapabilitiesTask.Params(forceRefresh = true))
    }

    override fun getNodeCapabilities(): NodeCapabilities {
        return nodeCapabilitiesDataSource.getNodeCapabilities()
                ?: NodeCapabilities()
    }

    override fun getNodeCapabilitiesLive(): LiveData<Optional<NodeCapabilities>> {
        return nodeCapabilitiesDataSource.getNodeCapabilitiesLive()
    }
}
