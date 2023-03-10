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
 *
 */

package org.sdn.android.sdk.api.session.room.powerlevels

sealed class Role(open val value: Int) : Comparable<Role> {
    object Admin : Role(100)
    object Moderator : Role(50)
    object Default : Role(0)
    data class Custom(override val value: Int) : Role(value)

    override fun compareTo(other: Role): Int {
        return value.compareTo(other.value)
    }

    companion object {

        // Order matters, default value should be checked after defined roles
        fun fromValue(value: Int, default: Int): Role {
            return when (value) {
                Admin.value -> Admin
                Moderator.value -> Moderator
                Default.value,
                default -> Default
                else -> Custom(value)
            }
        }
    }
}
