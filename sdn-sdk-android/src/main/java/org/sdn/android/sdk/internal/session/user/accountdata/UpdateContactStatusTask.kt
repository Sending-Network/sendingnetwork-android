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

import com.squareup.moshi.Json
import com.zhuinden.monarchy.Monarchy
import org.sdn.android.sdk.api.util.JsonDict
//import org.sdn.android.sdk.api.session.accountdata.UserAccountDataTypes
//import org.sdn.android.sdk.internal.database.model.CantactUserEntity
import org.sdn.android.sdk.internal.di.SessionDatabase
import org.sdn.android.sdk.internal.di.UserId
import org.sdn.android.sdk.internal.network.GlobalErrorReceiver
import org.sdn.android.sdk.internal.network.executeRequest
//import org.sdn.android.sdk.internal.session.sync.model.accountdata.CantactUsersContent
import org.sdn.android.sdk.internal.task.Task
import org.sdn.android.sdk.internal.session.user.UpdateContactStatusAPI


import javax.inject.Inject

internal interface UpdateContactStatusTask : Task<UpdateContactStatusTask.Params, JsonDict> {

    data class Params(
        val parameterAdd: Map<String, Any> = emptyMap(),
        val parameterRemove: Map<String, Any> = emptyMap(),
        )

}

internal class DefaultUpdateContactStatusTask @Inject constructor(
    private val updateContactStatusAPI: UpdateContactStatusAPI,
    @UserId private val userId: String,
    private val globalErrorReceiver: GlobalErrorReceiver
) : UpdateContactStatusTask {

    override suspend fun execute(params: UpdateContactStatusTask.Params) :JsonDict {
        println("userId $userId")
        println("addContact $params.parameterAdd")
        println("addContact $params.parameterRemove")

        if (params.parameterAdd.isNotEmpty()) {
            return executeRequest(globalErrorReceiver) {
                updateContactStatusAPI.updateContactStatusAdd(userId,params.parameterAdd);
            }
        }

       if (params.parameterRemove.isNotEmpty()) {
           return executeRequest(globalErrorReceiver) {
               println("userID=$userId ,params.parameterRemove=$params.parameterRemove")
               println("userID=${params.parameterRemove["contact_id"]  as String} ,params.is_room=${params.parameterRemove["is_room"] as Int}")
             //  2023-10-31 09:37:59.460 32355-32404 System.out              org.sdn.android.sdk.sample           I  userID=@sdn_a4a43324220196914c6e97c032da79038deda6a1:a4a43324220196914c6e97c032da79038deda6a1 ,params.parameterRemove=Params(parameterAdd={}, parameterRemove={contact_id=@sdn_7dc1c0acc5c08ddd57d06a4420ade8fd54206da1:7dc1c0acc5c08ddd57d06a4420ade8fd54206da1, is_room=1}).parameterRemove
               updateContactStatusAPI.updateContactStatusRemove(userId,params.parameterRemove);
           }
       }

        return mapOf()

    }
}
