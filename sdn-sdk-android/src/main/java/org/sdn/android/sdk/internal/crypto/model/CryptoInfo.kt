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

package org.sdn.android.sdk.internal.crypto.model

/**
 * Generic crypto info.
 * Can be a device (CryptoDeviceInfo), as well as a CryptoCrossSigningInfo (can be seen as a kind of virtual device)
 */
internal interface CryptoInfo {

    val userId: String

    val keys: Map<String, String>?

    val signatures: Map<String, Map<String, String>>?

    fun signalableJSONDictionary(): Map<String, Any>
}
