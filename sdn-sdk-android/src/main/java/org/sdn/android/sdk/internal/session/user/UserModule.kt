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

package org.sdn.android.sdk.internal.session.user

import dagger.Binds
import dagger.Module
import dagger.Provides
import org.sdn.android.sdk.api.session.user.UserService
import org.sdn.android.sdk.internal.session.SessionScope
import org.sdn.android.sdk.internal.session.user.accountdata.DefaultGetContactsListTask
import org.sdn.android.sdk.internal.session.user.accountdata.DefaultUpdateContactStatusTask
import org.sdn.android.sdk.internal.session.user.accountdata.DefaultUpdateIgnoredUserIdsTask
import org.sdn.android.sdk.internal.session.user.accountdata.GetContactsListTask
import org.sdn.android.sdk.internal.session.user.accountdata.UpdateContactStatusTask
import org.sdn.android.sdk.internal.session.user.accountdata.UpdateIgnoredUserIdsTask
import org.sdn.android.sdk.internal.session.user.model.DefaultSearchUserTask
import org.sdn.android.sdk.internal.session.user.model.SearchUserTask
import retrofit2.Retrofit

@Module
internal abstract class UserModule {

    @Module
    companion object {
        @Provides
        @JvmStatic
        @SessionScope
        fun providesSearchUserAPI(retrofit: Retrofit): SearchUserAPI {
            return retrofit.create(SearchUserAPI::class.java)
        }

        @Provides
        @JvmStatic
        @SessionScope
        fun providesUpdateContactStatusAPI(retrofit: Retrofit): UpdateContactStatusAPI {
            return retrofit.create(UpdateContactStatusAPI::class.java)
        }
    }

    @Binds
    abstract fun bindUserService(service: DefaultUserService): UserService

    @Binds
    abstract fun bindSearchUserTask(task: DefaultSearchUserTask): SearchUserTask

    @Binds
    abstract fun bindUpdateIgnoredUserIdsTask(task: DefaultUpdateIgnoredUserIdsTask): UpdateIgnoredUserIdsTask

    @Binds
    abstract fun bindUserStore(store: RealmUserStore): UserStore

    @Binds
    abstract fun bindGetContactsListTask(task: DefaultGetContactsListTask): GetContactsListTask

    @Binds
    abstract fun bindUpdateContactStatusTask(task: DefaultUpdateContactStatusTask): UpdateContactStatusTask
}
