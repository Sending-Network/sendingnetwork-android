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

package org.sdn.android.sdk.test.fakes

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.sdn.android.sdk.api.query.QueryStateEventValue
import org.sdn.android.sdk.api.session.events.model.Event
import org.sdn.android.sdk.internal.session.room.state.StateEventDataSource

internal class FakeStateEventDataSource {

    val instance: StateEventDataSource = mockk()

    fun givenGetStateEventReturns(event: Event?) {
        every {
            instance.getStateEvent(
                    roomId = any(),
                    eventType = any(),
                    stateKey = any()
            )
        } returns event
    }

    fun verifyGetStateEvent(roomId: String, eventType: String, stateKey: QueryStateEventValue) {
        verify {
            instance.getStateEvent(
                    roomId = roomId,
                    eventType = eventType,
                    stateKey = stateKey
            )
        }
    }
}
