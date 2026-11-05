package com.example.monoco.dados.remoto

import java.net.URI
import java.util.Locale

object ConfiguracaoServidor {
    const val ENDERECO_PADRAO = "http://10.0.2.2:8081/api/"

    fun normalizarEndereco(valor: String, permitirHttp: Boolean = true): String {
        val uri = runCatching { URI(valor.trim()) }.getOrNull()
        require(uri != null && !uri.host.isNullOrBlank()) { "Informe um endereço válido para o servidor." }
        val protocolo = uri.scheme.orEmpty().lowercase(Locale.ROOT)
        require(protocolo == "https" || protocolo == "http") { "Use um endereço HTTP ou HTTPS." }
        require(permitirHttp || protocolo == "https") { "Use HTTPS na versão de produção." }
        require(uri.userInfo == null && uri.query == null && uri.fragment == null) {
            "O endereço do servidor não deve conter credenciais, parâmetros ou fragmentos."
        }
        require(uri.port == -1 || uri.port in 1..65535) { "Informe uma porta entre 1 e 65535." }
        val caminho = uri.path.orEmpty().trimEnd('/')
        require(caminho.isEmpty() || caminho.endsWith("/api")) { "O endereço deve terminar em /api/." }
        return URI(
            protocolo, null, uri.host.lowercase(Locale.ROOT), uri.port,
            if (caminho.isEmpty()) "/api/" else "$caminho/", null, null
        ).toASCIIString()
    }
}
