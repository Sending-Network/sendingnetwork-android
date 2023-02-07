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
package org.sdn.android.sdk.internal.crypto.model.rest

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.sdn.android.sdk.api.session.crypto.model.GossipingToDeviceObject
import org.sdn.android.sdk.api.session.crypto.model.GossipingToDeviceObject.Companion.ACTION_SHARE_CANCELLATION

/**
 * Class representing a room key request cancellation content.
 */
@JsonClass(generateAdapter = true)
internal data class ShareRequestCancellation(
        @Json(name = "action")
        override val action: String? = ACTION_SHARE_CANCELLATION,

        @Json(name = "requesting_device_id")
        override val requestingDeviceId: String? = null,

        @Json(name = "request_id")
        override val requestId: String? = null
) : GossipingToDeviceObject
