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

package org.sdn.android.sdk.api.session.room.model.call

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.sdn.android.sdk.api.extensions.orFalse

@JsonClass(generateAdapter = true)
data class CallCapabilities(
        /**
         * If set to true, states that the sender of the event supports the m.call.replaces event and therefore supports
         * being transferred to another destination.
         */
        @Json(name = "m.call.transferee") val transferee: Boolean? = null
)

fun CallCapabilities?.supportCallTransfer() = this?.transferee.orFalse()
