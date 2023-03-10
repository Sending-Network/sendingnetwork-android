/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.sdn.android.sdk.api.rendezvous

import org.sdn.android.sdk.api.rendezvous.model.RendezvousError

/**
 * Representation of a rendezvous channel such as that described by MSC3903.
 */
interface RendezvousChannel {
    val transport: RendezvousTransport

    /**
     * @returns the checksum/confirmation digits to be shown to the user
     */
    @Throws(RendezvousError::class)
    suspend fun connect(): String

    /**
     * Send a payload via the channel.
     * @param data payload to send
     */
    @Throws(RendezvousError::class)
    suspend fun send(data: ByteArray)

    /**
     * Receive a payload from the channel.
     * @returns the received payload
     */
    @Throws(RendezvousError::class)
    suspend fun receive(): ByteArray?

    /**
     * Closes the channel and cleans up.
     */
    suspend fun close()
}
