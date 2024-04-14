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



package org.sdn.android.sdk.internal.session.user

import org.sdn.android.sdk.api.session.user.model.ContactsResponse
import org.sdn.android.sdk.api.util.JsonDict
import org.sdn.android.sdk.internal.network.NetworkConstants
import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Path
import retrofit2.http.Query

internal interface UpdateContactStatusAPI {

    /**
     * Set some account_data for the client.
     *
     * @param userId the user id
     * @param params the put params
     */
    @PUT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "contact/{userId}")
    suspend fun updateContactStatusAdd (
        @Path("userId") userId: String,
        @Body params: Any
    ) :JsonDict

    /**
     * Set some account_data for the client.
     *
     * @param userId the user id
     * @param params the put params
     */
    @HTTP(method = "DELETE", path = NetworkConstants.URI_API_PREFIX_PATH_R0 + "contact/{user_id}", hasBody = true)
    suspend fun updateContactStatusRemove(@Path("user_id") userId: String, @Body body: JsonDict) : JsonDict

    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "contacts_list/{user_id}")
    suspend fun getContactsList(@Path("user_id") userId: String) : ContactsResponse
}
