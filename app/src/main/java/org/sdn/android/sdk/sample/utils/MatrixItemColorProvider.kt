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

package org.sdn.android.sdk.sample.utils

import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import org.sdn.android.sdk.api.util.SDNItem
import org.sdn.android.sdk.sample.R
import kotlin.math.abs

class SDNItemColorProvider(private val context: Context) {
    private val cache = mutableMapOf<String, Int>()

    @ColorInt
    fun getColor(SDNItem: SDNItem): Int {
        return cache.getOrPut(SDNItem.id) {
            ContextCompat.getColor(
                context,
                when (SDNItem) {
                    is SDNItem.UserItem -> getColorFromUserId(SDNItem.id)
                    else -> getColorFromRoomId(SDNItem.id)
                }
            )
        }
    }

    fun getColorForRoom(id: String): Int {
        return cache.getOrPut(id) {
            ContextCompat.getColor(
                context,
                getColorFromRoomId(id)
            )
        }
    }

    companion object {
        @ColorRes
        @VisibleForTesting
        fun getColorFromUserId(userId: String?): Int {
            var hash = 0

            userId?.toList()?.map { chr -> hash = (hash shl 5) - hash + chr.code }

            return when (abs(hash) % 8) {
                1 -> R.color.username_2
                2 -> R.color.username_3
                3 -> R.color.username_4
                4 -> R.color.username_5
                5 -> R.color.username_6
                6 -> R.color.username_7
                7 -> R.color.username_8
                else -> R.color.username_1
            }
        }

        @ColorRes
        private fun getColorFromRoomId(roomId: String?): Int {
            return when ((roomId?.toList()?.sumOf { it.code } ?: 0) % 6) {
                1 -> R.color.avatar_fill_1
                2 -> R.color.avatar_fill_2
                3 -> R.color.avatar_fill_3
                4 -> R.color.avatar_fill_4
                5 -> R.color.avatar_fill_5
                else -> R.color.avatar_fill_6
            }
        }
    }
}
