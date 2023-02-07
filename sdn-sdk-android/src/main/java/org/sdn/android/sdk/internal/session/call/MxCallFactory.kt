/*
 * Copyright (c) 2020 The Matrix.org Foundation C.I.C.
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

package org.sdn.android.sdk.internal.session.call

import org.sdn.android.sdk.api.SDNConfiguration
import org.sdn.android.sdk.api.session.call.CallIdGenerator
import org.sdn.android.sdk.api.session.call.MxCall
import org.sdn.android.sdk.api.session.room.model.call.CallCapabilities
import org.sdn.android.sdk.api.session.room.model.call.CallInviteContent
import org.sdn.android.sdk.api.session.room.model.call.CallSignalingContent
import org.sdn.android.sdk.internal.di.DeviceId
import org.sdn.android.sdk.internal.di.UserId
import org.sdn.android.sdk.internal.session.call.model.MxCallImpl
import org.sdn.android.sdk.internal.session.profile.GetProfileInfoTask
import org.sdn.android.sdk.internal.session.room.send.LocalEchoEventFactory
import org.sdn.android.sdk.internal.session.room.send.queue.EventSenderProcessor
import org.sdn.android.sdk.internal.util.time.Clock
import javax.inject.Inject

internal class MxCallFactory @Inject constructor(
    @DeviceId private val deviceId: String?,
    private val localEchoEventFactory: LocalEchoEventFactory,
    private val eventSenderProcessor: EventSenderProcessor,
    private val sdnConfiguration: SDNConfiguration,
    private val getProfileInfoTask: GetProfileInfoTask,
    @UserId private val userId: String,
    private val clock: Clock,
) {

    fun createIncomingCall(roomId: String, opponentUserId: String, content: CallInviteContent): MxCall? {
        content.callId ?: return null
        return MxCallImpl(
                callId = content.callId,
                isOutgoing = false,
                roomId = roomId,
                userId = userId,
                ourPartyId = deviceId ?: "",
                isVideoCall = content.isVideo(),
                localEchoEventFactory = localEchoEventFactory,
                eventSenderProcessor = eventSenderProcessor,
                sdnConfiguration = sdnConfiguration,
                getProfileInfoTask = getProfileInfoTask,
                clock = clock,
        ).apply {
            updateOpponentData(opponentUserId, content, content.capabilities)
        }
    }

    fun createOutgoingCall(roomId: String, opponentUserId: String, isVideoCall: Boolean): MxCall {
        return MxCallImpl(
                callId = CallIdGenerator.generate(),
                isOutgoing = true,
                roomId = roomId,
                userId = userId,
                ourPartyId = deviceId ?: "",
                isVideoCall = isVideoCall,
                localEchoEventFactory = localEchoEventFactory,
                eventSenderProcessor = eventSenderProcessor,
                sdnConfiguration = sdnConfiguration,
                getProfileInfoTask = getProfileInfoTask,
                clock = clock,
        ).apply {
            // Setup with this userId, might be updated when processing the Answer event
            this.opponentUserId = opponentUserId
        }
    }

    fun updateOutgoingCallWithOpponentData(
            call: MxCall,
            userId: String,
            content: CallSignalingContent,
            callCapabilities: CallCapabilities?
    ) {
        (call as? MxCallImpl)?.updateOpponentData(userId, content, callCapabilities)
    }
}
