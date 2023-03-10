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

package org.sdn.android.sdk.api.session.room.alias

interface AliasService {
    /**
     * Get list of local alias of the room.
     * @return the list of the aliases (full aliases, not only the local part)
     */
    suspend fun getRoomAliases(): List<String>

    /**
     * Add local alias to the room.
     * @param aliasLocalPart the local part of the alias.
     * Ex: for the alias "#my_alias:example.org", the local part is "my_alias"
     */
    suspend fun addAlias(aliasLocalPart: String)
}
