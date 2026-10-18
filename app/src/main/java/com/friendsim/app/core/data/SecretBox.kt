package com.friendsim.app.core.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 本地数据加密：AES-256/GCM，密钥由 Android Keystore 生成并托管（不可导出、不落盘）。
 * 聊天记录整体加密后再写文件，密钥本身不进 SharedPreferences、不进代码。
 */
object SecretBox {

    private const val ALIAS = "friendsim_history_key"
    private const val TRANSFORM = "AES/GCM/NoPadding"

    private fun keyStore(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun key(): SecretKey {
        val store = keyStore()
        (store.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    /** 输出格式：[ivLen(1)] + iv + cipherText。 */
    fun encrypt(plain: ByteArray): ByteArray? = runCatching {
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = cipher.iv
        byteArrayOf(iv.size.toByte()) + iv + cipher.doFinal(plain)
    }.getOrNull()

    fun decrypt(payload: ByteArray): ByteArray? = runCatching {
        require(payload.size > 2)
        val ivLength = payload[0].toInt()
        val iv = payload.copyOfRange(1, 1 + ivLength)
        val body = payload.copyOfRange(1 + ivLength, payload.size)
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        cipher.doFinal(body)
    }.getOrNull()
}
