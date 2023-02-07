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

package org.sdn.android.sdk.internal.database.query

/**
 * Query strings used to filter the timeline events regarding the Json raw string of the Event.
 */
internal object TimelineEventFilter {
    /**
     * To apply to Event.content.
     */
    internal object Content {
        internal const val EDIT = """{*"m.relates_to"*"rel_type":*"m.replace"*}"""
        internal const val RESPONSE = """{*"m.relates_to"*"rel_type":*"org.matrix.response"*}"""
        internal const val REFERENCE = """{*"m.relates_to"*"rel_type":*"m.reference"*}"""
    }

    /**
     * To apply to Event.decryptionResultJson.
     */
    internal object DecryptedContent {
        internal const val URL = """{*"file":*"url":*}"""
    }

    /**
     * To apply to Event.unsigned.
     */
    internal object Unsigned {
        internal const val REDACTED = """{*"redacted_because":*}"""
    }
}
