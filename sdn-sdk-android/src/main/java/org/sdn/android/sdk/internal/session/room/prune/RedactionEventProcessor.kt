/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sdn.android.sdk.internal.session.room.prune

import io.realm.Realm
import org.sdn.android.sdk.api.session.events.model.Event
import org.sdn.android.sdk.api.session.events.model.EventType
import org.sdn.android.sdk.api.session.events.model.LocalEcho
import org.sdn.android.sdk.api.session.events.model.UnsignedData
import org.sdn.android.sdk.internal.database.helper.countInThreadMessages
import org.sdn.android.sdk.internal.database.helper.findRootThreadEvent
import org.sdn.android.sdk.internal.database.mapper.ContentMapper
import org.sdn.android.sdk.internal.database.mapper.EventMapper
import org.sdn.android.sdk.internal.database.model.EventEntity
import org.sdn.android.sdk.internal.database.model.EventInsertType
import org.sdn.android.sdk.internal.database.model.TimelineEventEntity
import org.sdn.android.sdk.internal.database.model.threads.ThreadSummaryEntity
import org.sdn.android.sdk.internal.database.query.findWithSenderMembershipEvent
import org.sdn.android.sdk.internal.database.query.where
import org.sdn.android.sdk.internal.di.MoshiProvider
import org.sdn.android.sdk.internal.session.EventInsertLiveProcessor
import timber.log.Timber
import javax.inject.Inject

/**
 * Listens to the database for the insertion of any redaction event.
 * As it will actually delete the content, it should be called last in the list of listener.
 */
internal class RedactionEventProcessor @Inject constructor() : EventInsertLiveProcessor {

    override fun shouldProcess(eventId: String, eventType: String, insertType: EventInsertType): Boolean {
        return eventType == EventType.REDACTION
    }

    override fun process(realm: Realm, event: Event) {
        pruneEvent(realm, event)
    }

    private fun pruneEvent(realm: Realm, redactionEvent: Event) {
        if (redactionEvent.redacts.isNullOrBlank()) {
            return
        }

        // Check that we know this event
        EventEntity.where(realm, eventId = redactionEvent.eventId ?: "").findFirst() ?: return

        val isLocalEcho = LocalEcho.isLocalEchoId(redactionEvent.eventId ?: "")
        Timber.v("Redact event for ${redactionEvent.redacts} localEcho=$isLocalEcho")

        val eventToPrune = EventEntity.where(realm, eventId = redactionEvent.redacts).findFirst() ?: return

        val typeToPrune = eventToPrune.type
        val stateKey = eventToPrune.stateKey
        val allowedKeys = computeAllowedKeys(typeToPrune)
        when {
            allowedKeys.isNotEmpty() -> {
                val prunedContent = ContentMapper.map(eventToPrune.content)?.filterKeys { key -> allowedKeys.contains(key) }
                eventToPrune.content = ContentMapper.map(prunedContent)
            }
            canPruneEventType(typeToPrune) -> {
                Timber.d("REDACTION for message ${eventToPrune.eventId}")
                val unsignedData = EventMapper.map(eventToPrune).unsignedData ?: UnsignedData(null, null)

                // was this event a m.replace
//                    val contentModel = ContentMapper.map(eventToPrune.content)?.toModel<MessageContent>()
//                    if (RelationType.REPLACE == contentModel?.relatesTo?.type && contentModel.relatesTo?.eventId != null) {
//                        eventRelationsAggregationUpdater.handleRedactionOfReplace(eventToPrune, contentModel.relatesTo!!.eventId!!, realm)
//                    }

                val modified = unsignedData.copy(redactedEvent = redactionEvent)
                eventToPrune.content = ContentMapper.map(emptyMap())
                eventToPrune.unsignedData = MoshiProvider.providesMoshi().adapter(UnsignedData::class.java).toJson(modified)
                eventToPrune.decryptionResultJson = null
                eventToPrune.decryptionErrorCode = null

                handleTimelineThreadSummaryIfNeeded(realm, eventToPrune, isLocalEcho)
            }
            else -> {
                Timber.w("Not pruning event (type $typeToPrune)")
            }
        }
        if (typeToPrune == EventType.STATE_ROOM_MEMBER && stateKey != null) {
            TimelineEventEntity.findWithSenderMembershipEvent(realm, eventToPrune.eventId).forEach {
                it.senderName = null
                it.isUniqueDisplayName = false
                it.senderAvatar = null
            }
        }
    }

    /**
     * Invalidates the number of threads in the main timeline thread summary,
     * with respect to redactions.
     */
    private fun handleTimelineThreadSummaryIfNeeded(
            realm: Realm,
            eventToPrune: EventEntity,
            isLocalEcho: Boolean,
    ) {
        if (eventToPrune.isThread() && !isLocalEcho) {
            val roomId = eventToPrune.roomId
            val rootThreadEvent = eventToPrune.findRootThreadEvent() ?: return
            val rootThreadEventId = eventToPrune.rootThreadEventId ?: return

            val inThreadMessages = countInThreadMessages(
                    realm = realm,
                    roomId = roomId,
                    rootThreadEventId = rootThreadEventId
            )

            rootThreadEvent.numberOfThreads = inThreadMessages
            if (inThreadMessages == 0) {
                // We should also clear the thread summary list
                rootThreadEvent.isRootThread = false
                rootThreadEvent.threadSummaryLatestMessage = null
                ThreadSummaryEntity
                        .where(realm, roomId = roomId, rootThreadEventId)
                        .findFirst()
                        ?.deleteFromRealm()
            }
        }
    }

    private fun computeAllowedKeys(type: String): List<String> {
        // Add filtered content, allowed keys in content depends on the event type
        return when (type) {
            EventType.STATE_ROOM_MEMBER -> listOf("membership")
            EventType.STATE_ROOM_CREATE -> listOf("creator")
            EventType.STATE_ROOM_JOIN_RULES -> listOf("join_rule")
            EventType.STATE_ROOM_POWER_LEVELS -> listOf(
                    "users",
                    "users_default",
                    "events",
                    "events_default",
                    "state_default",
                    "ban",
                    "kick",
                    "redact",
                    "invite"
            )
            EventType.STATE_ROOM_ALIASES -> listOf("aliases")
            EventType.STATE_ROOM_CANONICAL_ALIAS -> listOf("alias")
            EventType.FEEDBACK -> listOf("type", "target_event_id")
            else -> emptyList()
        }
    }

    private fun canPruneEventType(eventType: String): Boolean {
        return when {
            EventType.isCallEvent(eventType) -> false
            EventType.isVerificationEvent(eventType) -> false
            eventType == EventType.STATE_ROOM_WIDGET_LEGACY ||
                    eventType == EventType.STATE_ROOM_WIDGET ||
                    eventType == EventType.STATE_ROOM_NAME ||
                    eventType == EventType.STATE_ROOM_TOPIC ||
                    eventType == EventType.STATE_ROOM_AVATAR ||
                    eventType == EventType.STATE_ROOM_THIRD_PARTY_INVITE ||
                    eventType == EventType.STATE_ROOM_GUEST_ACCESS ||
                    eventType == EventType.STATE_SPACE_CHILD ||
                    eventType == EventType.STATE_SPACE_PARENT ||
                    eventType == EventType.STATE_ROOM_TOMBSTONE ||
                    eventType == EventType.STATE_ROOM_HISTORY_VISIBILITY ||
                    eventType == EventType.STATE_ROOM_RELATED_GROUPS ||
                    eventType == EventType.STATE_ROOM_PINNED_EVENT ||
                    eventType == EventType.STATE_ROOM_ENCRYPTION ||
                    eventType == EventType.STATE_ROOM_SERVER_ACL ||
                    eventType == EventType.REACTION -> false
            else -> true
        }
    }
}
