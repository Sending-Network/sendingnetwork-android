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

package org.sdn.android.sdk.api.util

import org.sdn.android.sdk.BuildConfig
import org.sdn.android.sdk.api.extensions.tryOrNull
import org.sdn.android.sdk.api.session.room.model.RoomMemberSummary
import org.sdn.android.sdk.api.session.room.model.RoomSummary
import org.sdn.android.sdk.api.session.room.model.RoomType
import org.sdn.android.sdk.api.session.room.model.SpaceChildInfo
import org.sdn.android.sdk.api.session.room.model.roomdirectory.PublicRoom
import org.sdn.android.sdk.api.session.room.sender.SenderInfo
import org.sdn.android.sdk.api.session.user.model.User
import java.util.Locale

sealed class SDNItem(
        open val id: String,
        open val displayName: String?,
        open val avatarUrl: String?
) {
    data class UserItem(
            override val id: String,
            override val displayName: String? = null,
            override val avatarUrl: String? = null
    ) :
            SDNItem(id, displayName?.removeSuffix(IRC_PATTERN), avatarUrl) {

        init {
            if (BuildConfig.DEBUG) checkId()
        }

        override fun updateAvatar(newAvatar: String?) = copy(avatarUrl = newAvatar)
    }

    data class EveryoneInRoomItem(
            override val id: String,
            override val displayName: String = NOTIFY_EVERYONE,
            override val avatarUrl: String? = null,
            val roomDisplayName: String? = null
    ) :
            SDNItem(id, displayName, avatarUrl) {
        init {
            if (BuildConfig.DEBUG) checkId()
        }

        override fun updateAvatar(newAvatar: String?) = copy(avatarUrl = newAvatar)
    }

    data class EventItem(
            override val id: String,
            override val displayName: String? = null,
            override val avatarUrl: String? = null
    ) :
            SDNItem(id, displayName, avatarUrl) {
        init {
            if (BuildConfig.DEBUG) checkId()
        }

        override fun updateAvatar(newAvatar: String?) = copy(avatarUrl = newAvatar)
    }

    data class RoomItem(
            override val id: String,
            override val displayName: String? = null,
            override val avatarUrl: String? = null
    ) :
            SDNItem(id, displayName, avatarUrl) {
        init {
            if (BuildConfig.DEBUG) checkId()
        }

        override fun updateAvatar(newAvatar: String?) = copy(avatarUrl = newAvatar)
    }

    data class SpaceItem(
            override val id: String,
            override val displayName: String? = null,
            override val avatarUrl: String? = null
    ) :
            SDNItem(id, displayName, avatarUrl) {
        init {
            if (BuildConfig.DEBUG) checkId()
        }

        override fun updateAvatar(newAvatar: String?) = copy(avatarUrl = newAvatar)
    }

    data class RoomAliasItem(
            override val id: String,
            override val displayName: String? = null,
            override val avatarUrl: String? = null
    ) :
            SDNItem(id, displayName, avatarUrl) {
        init {
            if (BuildConfig.DEBUG) checkId()
        }

        override fun updateAvatar(newAvatar: String?) = copy(avatarUrl = newAvatar)
    }

    protected fun checkId() {
        if (!id.startsWith(getIdPrefix())) {
            error("Wrong usage of SDNItem: check the id $id should start with ${getIdPrefix()}")
        }
    }

    abstract fun updateAvatar(newAvatar: String?): SDNItem

    /**
     * Return the prefix as defined in the matrix spec (and not extracted from the id).
     */
    private fun getIdPrefix() = when (this) {
        is UserItem -> '@'
        is EventItem -> '$'
        is SpaceItem,
        is RoomItem,
        is EveryoneInRoomItem -> '!'
        is RoomAliasItem -> '#'
    }

    fun firstLetterOfDisplayName(): String {
        val displayName = when (this) {
            // use the room display name for the notify everyone item
            is EveryoneInRoomItem -> roomDisplayName
            else -> displayName
        }
        return (displayName?.takeIf { it.isNotBlank() } ?: id)
                .let { dn ->
                    var startIndex = 0
                    val initial = dn[startIndex]

                    if (initial in listOf('@', '#', '+') && dn.length > 1) {
                        startIndex++
                    }

                    var length = 1
                    var first = dn[startIndex]

                    // LEFT-TO-RIGHT MARK
                    if (dn.length >= 2 && 0x200e == first.code) {
                        startIndex++
                        first = dn[startIndex]
                    }

                    // check if it’s the start of a surrogate pair
                    if (first.code in 0xD800..0xDBFF && dn.length > startIndex + 1) {
                        val second = dn[startIndex + 1]
                        if (second.code in 0xDC00..0xDFFF) {
                            length++
                        }
                    }

                    dn.substring(startIndex, startIndex + length)
                }
                .uppercase(Locale.ROOT)
    }

    companion object {
        private const val IRC_PATTERN = " (IRC)"
        const val NOTIFY_EVERYONE = "@room"
    }
}

/* ==========================================================================================
 * Extensions to create MatrixItem
 * ========================================================================================== */

fun User.toSDNItem() = SDNItem.UserItem(userId, displayName, avatarUrl)

fun RoomSummary.toSDNItem() = if (roomType == RoomType.SPACE) {
    SDNItem.SpaceItem(roomId, displayName, avatarUrl)
} else {
    SDNItem.RoomItem(roomId, displayName, avatarUrl)
}

fun RoomSummary.toRoomAliasSDNItem() = SDNItem.RoomAliasItem(canonicalAlias ?: roomId, displayName, avatarUrl)

fun RoomSummary.toEveryoneInRoomSDNItem() = SDNItem.EveryoneInRoomItem(id = roomId, avatarUrl = avatarUrl, roomDisplayName = displayName)

// If no name is available, use room alias as Riot-Web does
fun PublicRoom.toSDNItem() = SDNItem.RoomItem(roomId, name ?: getPrimaryAlias() ?: "", avatarUrl)

fun RoomMemberSummary.toSDNItem() = SDNItem.UserItem(userId, displayName, avatarUrl)

fun SenderInfo.toSDNItem() = SDNItem.UserItem(userId, disambiguatedDisplayName, avatarUrl)

fun SenderInfo.toSDNItemOrNull() = tryOrNull { SDNItem.UserItem(userId, disambiguatedDisplayName, avatarUrl) }

fun SpaceChildInfo.toSDNItem() = if (roomType == RoomType.SPACE) {
    SDNItem.SpaceItem(childRoomId, name ?: canonicalAlias, avatarUrl)
} else {
    SDNItem.RoomItem(childRoomId, name ?: canonicalAlias, avatarUrl)
}
