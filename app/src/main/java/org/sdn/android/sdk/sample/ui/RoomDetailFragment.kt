/*
 * Copyright (c) 2020 New Vector Ltd
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

package org.sdn.android.sdk.sample.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.stfalcon.chatkit.commons.ImageLoader
import com.stfalcon.chatkit.commons.models.IMessage
import com.stfalcon.chatkit.messages.MessageInput
import com.stfalcon.chatkit.messages.MessagesListAdapter
import kotlinx.coroutines.launch
import org.sdn.android.sdk.api.extensions.orTrue
import org.sdn.android.sdk.api.session.getRoom
import org.sdn.android.sdk.api.session.room.Room
import org.sdn.android.sdk.api.session.room.read.ReadService
import org.sdn.android.sdk.api.session.room.timeline.*
import org.sdn.android.sdk.api.util.toSDNItem
import org.sdn.android.sdk.sample.SessionHolder
import org.sdn.android.sdk.sample.databinding.FragmentRoomDetailBinding
import org.sdn.android.sdk.sample.utils.*

import org.sdn.android.sdk.api.meet.SdnMeetActivity
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
//import com.github.zhanghai.android.kotlin.BaseEncoding
import org.apache.commons.codec.binary.Base32

class RoomDetailFragment : Fragment(), Timeline.Listener, ToolbarConfigurable {

    companion object {

        private const val ROOM_ID_ARGS = "ROOM_ID_ARGS"

        fun newInstance(roomId: String): RoomDetailFragment {
            val args = bundleOf(
                    Pair(ROOM_ID_ARGS, roomId)
            )
            return RoomDetailFragment().apply {
                arguments = args
            }
        }
    }

    private var _views: FragmentRoomDetailBinding? = null
    private val views get() = _views!!

    private val session = SessionHolder.currentSession!!
    private var timeline: Timeline? = null
    private var room: Room? = null

    private val avatarRenderer by lazy {
        AvatarRenderer(this, SDNItemColorProvider(requireContext()))
    }

    private val imageLoader = ImageLoader { imageView, url, _ ->
        avatarRenderer.render(url, imageView)
    }

    private val adapter = MessagesListAdapter<IMessage>(session.myUserId, imageLoader)
    private val timelineEventListProcessor = TimelineEventListProcessor(adapter)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _views = FragmentRoomDetailBinding.inflate(inflater, container, false)
        return views.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureToolbar(views.toolbar, displayBack = true)
        views.textComposer.setInputListener {
            // Sending message can be as simple as that.
            // Timeline will be automatically updated with local echo
            // and when receiving from sync so you don't have anything else to do
            room?.sendService()?.sendTextMessage(it)
            true
        }

        views.textComposer.setTypingListener(object : MessageInput.TypingListener {
            override fun onStartTyping() {
                room?.typingService()?.userIsTyping()
            }

            override fun onStopTyping() {
                room?.typingService()?.userStopsTyping()
            }
        })

        views.timelineEventList.setAdapter(adapter)
        views.timelineEventList.itemAnimator = null
        views.timelineEventList.addOnScrollListener(RecyclerScrollMoreListener(views.timelineEventList.layoutManager as LinearLayoutManager) {
            if (timeline?.hasMoreToLoad(Timeline.Direction.BACKWARDS).orTrue()) {
                timeline?.paginate(Timeline.Direction.BACKWARDS, 50)
            }
        })
        val roomID = arguments?.getString(ROOM_ID_ARGS)!!
        // You can grab a room from the session
        // If the room is not known (not received from sync) it will return null
        room = session.getRoom(roomID)

        lifecycleScope.launch {
            room?.readService()?.markAsRead(ReadService.MarkAsReadParams.READ_RECEIPT)
        }

        // Create some settings to configure timeline
        val timelineSettings = TimelineSettings(
                initialSize = 30
        )
        // Then you can retrieve a timeline from this room.
        timeline = room?.timelineService()?.createTimeline(null, timelineSettings)?.also {
            // Don't forget to add listener and start the timeline so it start listening to changes
            it.addListener(this)
            it.start()
        }

        // You can also listen to room summary from the room
        room?.getRoomSummaryLive()?.observe(viewLifecycleOwner) { roomSummary ->
            val roomSummaryAsSDNItem =
                    roomSummary.map { it.toSDNItem() }.getOrNull() ?: return@observe
            avatarRenderer.render(roomSummaryAsSDNItem, views.toolbarAvatarImageView)
            views.toolbarTitleView.text = roomSummaryAsSDNItem.let {
                it.displayName?.takeIf { dn -> dn.isNotBlank() } ?: it.id
            }
        }

        views.toolbarBtnVideo.setOnClickListener {
            Log.d("getMeeting","start")
            GlobalScope.launch {
                joinRoomMeeting(context!!, roomID)
            }
        }
    }

    override fun onDestroyView() {
        timeline?.also {
            // Don't forget to remove listener and dispose timeline to avoid memory leaks
            it.removeAllListeners()
            it.dispose()
        }
        timeline = null
        room = null
        super.onDestroyView()
    }

    override fun onNewTimelineEvents(eventIds: List<String>) {
        // This is new event ids coming from sync
    }

    override fun onTimelineFailure(throwable: Throwable) {
        // When a failure is happening when trying to retrieve an event.
        // This is an unrecoverable error, you might want to restart the timeline
        // timeline?.restartWithEventId("")
    }

    override fun onTimelineUpdated(snapshot: List<TimelineEvent>) {
        // Each time the timeline is updated it will be called.
        // It can happens when sync returns, paginating, and updating (local echo, decryption finished...)
        // You probably want to process with DiffUtil before dispatching to your recyclerview
        timelineEventListProcessor.onNewSnapshot(snapshot)
    }

    private suspend fun joinRoomMeeting(context: Context, roomId: String) {
        try {
            val url = session.roomService().getMeetingURL()

            if (url.serverUrl != null) {
                CoroutineScope(Dispatchers.Main).launch {
                    val conferenceId = encodedString(roomId)
                    println("conferenceId=${conferenceId} ")
                    println("serverUrl=${url.serverUrl.toString()} ")

                    SdnMeetActivity.launch(context, conferenceId, url.serverUrl.toString())
                }
            }
        } catch (e: Exception) {
            println("meeting error : $e")
        }
    }
    private fun encodedString(string: String, padding: Boolean = false): String {
        val base32 = Base32()
        val encodedBytes = base32.encode(string.toByteArray())

        val encodedString = String(encodedBytes)

        return if (padding) {
            encodedString
        } else {
            encodedString.replace("=", "")
        }
    }


}
