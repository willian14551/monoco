package com.example.monoco.dados.remoto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

data class ContaOnline(val id: Long, val nome: String, val email: String)

data class SessaoOnline(val token: String, val expiraEm: Long, val usuario: ContaOnline)

class SessaoSegura(contexto: Context) {
    private val preferencias = contexto.getSharedPreferences("sessao_online", Context.MODE_PRIVATE)
    var endereco: String
        get() = preferencias.getString("endereco", "http://10.0.2.2:8080/api/")!!
        set(valor) {
            preferencias.edit().putString("endereco", valor.trim().trimEnd('/') + "/").apply()
        }

    private fun chave(): SecretKey {
        val cofre = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (cofre.getKey("monoco_sessao", null) as? SecretKey)?.let {
            return it
        }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply {
                init(
                    KeyGenParameterSpec.Builder(
                            "monoco_sessao",
                            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                        )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build()
                )
            }
            .generateKey()
    }

    fun salvar(sessao: SessaoOnline) {
        val json =
            JSONObject()
                .put("token", sessao.token)
                .put("expiraEm", sessao.expiraEm)
                .put("id", sessao.usuario.id)
                .put("nome", sessao.usuario.nome)
                .put("email", sessao.usuario.email)
        val cifra =
            Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, chave()) }
        val dados = cifra.iv + cifra.doFinal(json.toString().toByteArray(Charsets.UTF_8))
        preferencias
            .edit()
            .putString("sessao", Base64.encodeToString(dados, Base64.NO_WRAP))
            .apply()
    }

    fun ler(): SessaoOnline? =
        try {
            val salvo = preferencias.getString("sessao", null)
            if (salvo == null) null
            else {
                val dados = Base64.decode(salvo, Base64.NO_WRAP)
                val cifra =
                    Cipher.getInstance("AES/GCM/NoPadding").apply {
                        init(
                            Cipher.DECRYPT_MODE,
                            chave(),
                            GCMParameterSpec(128, dados.copyOfRange(0, 12)),
                        )
                    }
                val json =
                    JSONObject(
                        String(cifra.doFinal(dados.copyOfRange(12, dados.size)), Charsets.UTF_8)
                    )
                if (json.getLong("expiraEm") <= System.currentTimeMillis()) {
                    limpar()
                    null
                } else
                    SessaoOnline(
                        json.getString("token"),
                        json.getLong("expiraEm"),
                        ContaOnline(
                            json.getLong("id"),
                            json.getString("nome"),
                            json.getString("email"),
                        ),
                    )
            }
        } catch (_: Exception) {
            limpar()
            null
        }

    fun limpar() {
        preferencias.edit().remove("sessao").apply()
    }
}
