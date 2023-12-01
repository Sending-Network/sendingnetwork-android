package org.sdn.android.sdk.internal.crypto.algorithms.megolm

import android.text.TextUtils
import android.util.Base64
import timber.log.Timber
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class AESUtil {
    companion object {
        private const val algorithm = "AES"
        private const val cipherMode = "AES/CBC/PKCS7Padding" //algorithm/mode/padding

        fun encrypt(key: ByteArray, cleartext: String): String {
            Timber.i("AES encrypt with key: ${key.decodeToString()}")
            if (TextUtils.isEmpty(cleartext)) {
                return cleartext
            }
            val cipher: Cipher = Cipher.getInstance(cipherMode)
            val keySpec = SecretKeySpec(key, algorithm)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(key.copyOfRange(0, cipher.blockSize)))
            val encryptedBytes =  cipher.doFinal(cleartext.toByteArray())

            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        }

        fun decrypt(key: ByteArray, ciphertext: String): String {
            Timber.i("AES decrypt with key: ${key.decodeToString()}")
            if (TextUtils.isEmpty(ciphertext)) {
                return ciphertext
            }
            val encryptedBytes = Base64.decode(ciphertext, Base64.DEFAULT)
            val cipher: Cipher = Cipher.getInstance(cipherMode)
            val keySpec = SecretKeySpec(key, algorithm)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(key.copyOfRange(0, cipher.blockSize)))
            val decryptedBytes = cipher.doFinal(encryptedBytes)

            return decryptedBytes.decodeToString()
        }
    }

}