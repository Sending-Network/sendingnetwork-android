/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.sdn.android.sdk.api.session.crypto.keysbackup

sealed interface KeysBackupLastVersionResult {
    // No Keys backup found (404 error)
    object NoKeysBackup : KeysBackupLastVersionResult
    data class KeysBackup(val keysVersionResult: KeysVersionResult) : KeysBackupLastVersionResult
}

fun KeysBackupLastVersionResult.toKeysVersionResult(): KeysVersionResult? = when (this) {
    is KeysBackupLastVersionResult.KeysBackup -> keysVersionResult
    KeysBackupLastVersionResult.NoKeysBackup -> null
}
