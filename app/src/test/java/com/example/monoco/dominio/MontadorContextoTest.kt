package com.example.monoco.dominio

import org.junit.Assert.*
import org.junit.Test

class MontadorContextoTest {
    private fun montar(campos: List<CampoDoPersonagem> = emptyList(),
        memorias: List<MemoriaDoContexto> = emptyList(),
        historico: List<MensagemDoContexto> = emptyList()) = MontadorContexto.montar(
        regras = "Lia é uma cartógrafa aliada.", campos = campos, memorias = memorias,
        historico = historico, tom = "Aventura", tamanhoResposta = "Média", criatividade = 0.7f)

    @Test fun segredoOcultoNaoVazaParaOContexto() {
        val contexto = montar(campos = listOf(
            CampoDoPersonagem("Nome", "Aurel", true),
            CampoDoPersonagem("Segredos", "Identidade que não pode ser enviada", false)))
        assertEquals(mapOf("Nome" to "Aurel"), contexto.camposPersonagem)
        assertFalse(contexto.toString().contains("Identidade que não pode ser enviada"))
    }

    @Test fun nomeTambemPodeSerOcultado() {
        assertTrue(montar(campos = listOf(CampoDoPersonagem("Nome", "Aurel", false)))
            .camposPersonagem.isEmpty())
    }

    @Test fun memoriaInativaNaoVaiParaONarrador() {
        val contexto = montar(memorias = listOf(
            MemoriaDoContexto("Lia é uma aliada", true), MemoriaDoContexto("Informação antiga", false)))
        assertEquals(listOf("Lia é uma aliada"), contexto.memorias)
    }

    @Test fun historicoPreservaOrdemEAsVinteMensagensMaisRecentes() {
        val historico = (1..35).map { MensagemDoContexto("jogador", "Ação $it") }
        val contexto = montar(historico = historico)
        assertEquals(20, contexto.historico.size)
        assertEquals("Ação 16", contexto.historico.first().texto)
        assertEquals("Ação 35", contexto.historico.last().texto)
        assertEquals(35, historico.size)
        assertEquals("Lia é uma cartógrafa aliada.", contexto.regras)
    }
}
