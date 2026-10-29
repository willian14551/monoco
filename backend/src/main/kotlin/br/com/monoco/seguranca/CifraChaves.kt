package br.com.monoco.seguranca

import java.nio.charset.StandardCharsets.UTF_8
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class CifraChaves(@Value("\${monoco.encryption-key}") segredo: String) {
    private val chave =
        Base64.getDecoder()
            .decode(segredo)
            .also {
                require(it.size == 32) {
                    "ENCRYPTION_KEY deve conter 32 bytes codificados em Base64."
                }
            }
            .let { SecretKeySpec(it, "AES") }

    fun cifrar(texto: String): String {
        val nonce = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cifra = Cipher.getInstance("AES/GCM/NoPadding")
        cifra.init(Cipher.ENCRYPT_MODE, chave, GCMParameterSpec(128, nonce))
        return Base64.getEncoder().encodeToString(nonce + cifra.doFinal(texto.toByteArray(UTF_8)))
    }

    fun decifrar(texto: String): String {
        val dados = Base64.getDecoder().decode(texto)
        val cifra = Cipher.getInstance("AES/GCM/NoPadding")
        cifra.init(Cipher.DECRYPT_MODE, chave, GCMParameterSpec(128, dados.copyOfRange(0, 12)))
        return String(cifra.doFinal(dados.copyOfRange(12, dados.size)), UTF_8)
    }
}
