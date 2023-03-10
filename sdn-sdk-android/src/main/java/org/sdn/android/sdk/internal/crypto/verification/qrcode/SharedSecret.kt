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

package org.sdn.android.sdk.internal.crypto.verification.qrcode

import org.sdn.android.sdk.api.util.toBase64NoPadding
import java.security.SecureRandom

internal fun generateSharedSecretV2(): String {
    val secureRandom = SecureRandom()

    // 8 bytes long
    val secretBytes = ByteArray(8)
    secureRandom.nextBytes(secretBytes)
    return secretBytes.toBase64NoPadding()
}
