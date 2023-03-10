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

package org.sdn.android.sdk.test.fakes

import org.amshove.kluent.shouldBeEqualTo
import org.sdn.android.sdk.internal.session.pushers.GetPushersResponse
import org.sdn.android.sdk.internal.session.pushers.JsonPusher
import org.sdn.android.sdk.internal.session.pushers.PushersAPI

internal class FakePushersAPI : PushersAPI {

    private var setRequestPayload: JsonPusher? = null
    private var error: Throwable? = null

    override suspend fun getPushers(): GetPushersResponse {
        TODO("Not yet implemented")
    }

    override suspend fun setPusher(jsonPusher: JsonPusher) {
        error?.let { throw it }
        setRequestPayload = jsonPusher
    }

    fun verifySetPusher(payload: JsonPusher) {
        this.setRequestPayload shouldBeEqualTo payload
    }

    fun givenSetPusherErrors(error: Throwable) {
        this.error = error
    }
}
