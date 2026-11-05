package com.example.monoco.dados.remoto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ConfiguracaoServidorTest {
    @Test
    fun enderecoPadraoApontaParaDockerNoEmulador() {
        assertEquals("http://10.0.2.2:8081/api/", ConfiguracaoServidor.ENDERECO_PADRAO)
    }

    @Test
    fun enderecoSemCaminhoRecebePrefixoDaApi() {
        assertEquals("http://192.168.0.10:8081/api/", ConfiguracaoServidor.normalizarEndereco(" http://192.168.0.10:8081 "))
    }

    @Test
    fun enderecoComPrefixoPreservaPortaECaminho() {
        assertEquals("https://exemplo.com/monoco/api/", ConfiguracaoServidor.normalizarEndereco("https://EXEMPLO.com/monoco/api"))
    }

    @Test
    fun caminhoIncorretoNaoPodeSerSalvo() {
        assertThrows(IllegalArgumentException::class.java) { ConfiguracaoServidor.normalizarEndereco("http://localhost:8081/personagens") }
    }

    @Test
    fun credenciaisParametrosEFragmentosSaoRecusados() {
        listOf("http://usuario:senha@localhost:8081/api/", "http://localhost:8081/api/?token=123", "http://localhost:8081/api/#inicio").forEach { endereco ->
            assertThrows(IllegalArgumentException::class.java) { ConfiguracaoServidor.normalizarEndereco(endereco) }
        }
    }

    @Test
    fun enderecoOuPortaInvalidosSaoRecusados() {
        listOf("", "localhost:8081", "ftp://localhost/api/", "http://localhost:0/api/", "http://localhost:65536/api/").forEach { endereco ->
            assertThrows(IllegalArgumentException::class.java) { ConfiguracaoServidor.normalizarEndereco(endereco) }
        }
    }

    @Test
    fun producaoExigeHttps() {
        assertThrows(IllegalArgumentException::class.java) { ConfiguracaoServidor.normalizarEndereco("http://localhost:8081/api/", permitirHttp = false) }
        assertEquals("https://exemplo.com/api/", ConfiguracaoServidor.normalizarEndereco("https://exemplo.com", permitirHttp = false))
    }
}
