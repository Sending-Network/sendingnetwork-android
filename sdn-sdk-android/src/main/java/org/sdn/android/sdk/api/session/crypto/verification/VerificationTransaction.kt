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

package org.sdn.android.sdk.api.session.crypto.verification

interface VerificationTransaction {

    var state: VerificationTxState

    val transactionId: String
    val otherUserId: String
    var otherDeviceId: String?

    // TODO Not used. Remove?
    val isIncoming: Boolean

    /**
     * User wants to cancel the transaction.
     */
    fun cancel()

    fun cancel(code: CancelCode)

    fun isToDeviceTransport(): Boolean
}
