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

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.stfalcon.chatkit.commons.ImageLoader
import com.stfalcon.chatkit.dialogs.DialogsListAdapter
import kotlinx.coroutines.launch
import org.sdn.android.sdk.api.session.room.model.Membership
import org.sdn.android.sdk.api.session.room.model.RoomSummary
import org.sdn.android.sdk.api.session.room.roomSummaryQueryParams
import org.sdn.android.sdk.api.util.toSDNItem
import org.sdn.android.sdk.sample.R
import org.sdn.android.sdk.sample.SessionHolder
import org.sdn.android.sdk.sample.data.RoomSummaryDialogWrapper
import org.sdn.android.sdk.sample.databinding.FragmentRoomListBinding
import org.sdn.android.sdk.sample.formatter.RoomListDateFormatter
import org.sdn.android.sdk.sample.utils.AvatarRenderer
import org.sdn.android.sdk.sample.utils.SDNItemColorProvider
import timber.log.Timber

class RoomListFragment : Fragment(), ToolbarConfigurable {

    private val session = SessionHolder.currentSession!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _views = FragmentRoomListBinding.inflate(inflater, container, false)
        return views.root
    }

    private var _views: FragmentRoomListBinding? = null
    private val views get() = _views!!

//    private val update:
    private val avatarRenderer by lazy {
        AvatarRenderer(this, SDNItemColorProvider(requireContext()))
    }

    private val imageLoader = ImageLoader { imageView, url, _ ->
        avatarRenderer.render(url, imageView)
    }
    private val roomAdapter = DialogsListAdapter<RoomSummaryDialogWrapper>(imageLoader)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureToolbar(views.toolbar, displayBack = false)

        views.createRoomButton.setOnClickListener {
            val userId = views.otherUserIdField.text.toString().trim()

            viewLifecycleOwner.lifecycleScope.launch {
                val roomService = session.roomService()
                val roomId = roomService.getExistingDirectRoomWithUser(otherUserId = userId)
                if (roomId.isNullOrEmpty()) {
                    // create new direct room
                    roomService.createDirectRoom(otherUserId = userId)
                } else if (roomService.getRoom(roomId)?.roomSummary()?.isInvisible == true) {
                    // show existing direct room
                    roomService.showRoom(roomId)
                } else {
                    Timber.w("room already exists: %s", roomId)
                }
            }
        //            GlobalScope.launch {
//                println("contact-signOut out")
//                val dictionary = mutableMapOf("contact_id" to "@sdn_7dc1c0acc5c08ddd57d06a4420ade8fd54206da1:7dc1c0acc5c08ddd57d06a4420ade8fd54206da1", "is_room" to 1)
//
//                try{
//                    val response = session.userService().removeContact(parameter = dictionary)
//                    println("contact response=$response")
//                }catch (t :Throwable){
//                    val errorMessage = t.toString()
//                    println("contact error=$errorMessage")
//                }
//            }
        }
        views.roomSummaryList.setAdapter(roomAdapter)
        roomAdapter.setDatesFormatter(RoomListDateFormatter())
        roomAdapter.setOnDialogClickListener {
            if( it.roomSummary.membership == Membership.INVITE) {
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle(it.roomSummary.name)
                builder.setMessage("Do you want to join this room?")
                builder.setPositiveButton("Join") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        session.roomService().joinRoom(it.roomSummary.roomId);
                        showRoomDetail(it.roomSummary)
                    }
                }
                builder.setNegativeButton("Cancel", null)
                val dialog = builder.create()
                dialog.setOnShowListener { _ ->
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(resources.getColor(R.color.dark_gray))
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(resources.getColor(R.color.dark_gray))
                }
                dialog.show()
            } else {
                showRoomDetail(it.roomSummary)
            }
        }

        roomAdapter.setOnDialogLongClickListener {
            if (it.roomSummary.membership == Membership.JOIN) {
                val items = arrayOf("Leave", "Delete")
                val listDialog = AlertDialog.Builder(requireContext())

                listDialog.setItems(items) { _, which -> // `which` starts from 0
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            when (which) {
                                0 -> session.roomService().leaveRoom(it.roomSummary.roomId)
                                1 -> session.roomService().hideRoom(it.roomSummary.roomId)
                            }
                        } catch (failure: Throwable) {
                            Timber.e(failure, "fail to ${items[which]}")
                            Toast.makeText(requireContext(), "fail to ${items[which]}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                listDialog.show()
            }
        }

        // Create query to listen to room summary list
        val roomSummariesQuery = roomSummaryQueryParams {
            memberships = Membership.activeMemberships()
        }
        // Then you can subscribe to livedata..
        session.roomService().getRoomSummariesLive(roomSummariesQuery).observe(viewLifecycleOwner) {
            // ... And refresh your adapter with the list. It will be automatically updated when an item of the list is changed.
            updateRoomList(it)
        }

        // You can also listen to user. Here we listen to ourself to get our avatar
        session.userService().getUserLive(session.myUserId).observe(viewLifecycleOwner) { user ->
            val userSDNItem = user.map { it.toSDNItem() }.getOrNull() ?: return@observe
            avatarRenderer.render(userSDNItem, views.toolbarAvatarImageView)
        }


        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.logout -> {
                signOut()
                true
            }
            R.id.test_action -> {
                testAction()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun signOut() {
        lifecycleScope.launch {
            try {
                session.signOutService().signOut(true)
            } catch (failure: Throwable) {
                activity?.let {
                    Toast.makeText(it, "Failure: $failure", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            SessionHolder.currentSession = null
            val fragment = SimpleLoginFragment()
            requireActivity().supportFragmentManager
                .beginTransaction().replace(R.id.fragmentContainer, fragment).commit()
//            activity?.finish()
        }
    }

    private fun testAction() {
        lifecycleScope.launch {
            val contacts = session.userService().getContacts()
            Timber.i("get contacts: %s", contacts)
        }
    }

    private fun showRoomDetail(roomSummary: RoomSummary) {
        val roomDetailFragment = RoomDetailFragment.newInstance(roomSummary.roomId)
        (activity as MainActivity).supportFragmentManager
            .beginTransaction()
            .addToBackStack(null)
            .replace(R.id.fragmentContainer, roomDetailFragment)
            .commit()
    }

    private fun updateRoomList(roomSummaryList: List<RoomSummary>?) {
        if (roomSummaryList == null) return
        val sortedRoomSummaryList = roomSummaryList.sortedByDescending {
            it.latestPreviewableEvent?.root?.originServerTs
        }.map {
            RoomSummaryDialogWrapper(it)
        }
        roomAdapter.setItems(sortedRoomSummaryList)
    }
}
