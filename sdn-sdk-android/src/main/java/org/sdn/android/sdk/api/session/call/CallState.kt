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

package org.sdn.android.sdk.api.session.call

import org.sdn.android.sdk.api.session.room.model.call.EndCallReason

sealed class CallState {

    /** Idle, setting up objects. */
    object Idle : CallState()

    /**
     * CreateOffer. Intermediate state between Idle and Dialing.
     */
    object CreateOffer : CallState()

    /** Dialing.  Outgoing call is signaling the remote peer */
    object Dialing : CallState()

    /** Local ringing. Incoming call offer received */
    object LocalRinging : CallState()

    /** Answering.  Incoming call is responding to remote peer */
    object Answering : CallState()

    /**
     * Connected. Incoming/Outgoing call, ice layer connecting or connected
     * Notice that the PeerState failed is not always final, if you switch network, new ice candidtates
     * could be exchanged, and the connection could go back to connected
     * */
    data class Connected(val iceConnectionState: MxPeerConnectionState) : CallState()

    /** Ended.  Incoming/Outgoing call, the call is terminated */
    data class Ended(val reason: EndCallReason? = null) : CallState()
}
