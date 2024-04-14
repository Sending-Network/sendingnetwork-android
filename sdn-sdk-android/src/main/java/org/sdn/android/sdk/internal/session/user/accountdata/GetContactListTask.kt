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

package org.sdn.android.sdk.internal.session.user.accountdata

import org.sdn.android.sdk.api.session.user.model.ContactsResponse
import org.sdn.android.sdk.api.util.JsonDict
import org.sdn.android.sdk.internal.di.UserId
import org.sdn.android.sdk.internal.network.GlobalErrorReceiver
import org.sdn.android.sdk.internal.network.executeRequest
import org.sdn.android.sdk.internal.session.user.UpdateContactStatusAPI
import org.sdn.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface GetContactsListTask : Task<Unit, ContactsResponse>

internal class DefaultGetContactsListTask @Inject constructor(
    private val updateContactStatusAPI: UpdateContactStatusAPI,
    @UserId private val userId: String,
    private val globalErrorReceiver: GlobalErrorReceiver
) : GetContactsListTask {

    override suspend fun execute(params: Unit) : ContactsResponse {
        return executeRequest(globalErrorReceiver) {
            updateContactStatusAPI.getContactsList(userId)
        }
    }
}
