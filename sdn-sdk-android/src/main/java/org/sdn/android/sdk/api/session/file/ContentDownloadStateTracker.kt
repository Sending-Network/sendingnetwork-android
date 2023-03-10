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

package org.sdn.android.sdk.api.session.file

interface ContentDownloadStateTracker {
    fun track(key: String, updateListener: UpdateListener)
    fun unTrack(key: String, updateListener: UpdateListener)
    fun clear()

    sealed class State {
        object Idle : State()
        data class Downloading(val current: Long, val total: Long, val indeterminate: Boolean) : State()
        object Decrypting : State()
        object Success : State()
        data class Failure(val errorCode: Int) : State()
    }

    interface UpdateListener {
        fun onDownloadStateUpdate(state: State)
    }
}
