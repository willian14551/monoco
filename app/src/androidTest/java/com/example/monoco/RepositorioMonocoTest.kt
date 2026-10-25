package com.example.monoco

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.monoco.dados.RepositorioMonoco
import com.example.monoco.dados.local.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositorioMonocoTest {
    private lateinit var banco: BancoMonoco
    private lateinit var repositorio: RepositorioMonoco

    @Before fun preparar() {
        banco = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), BancoMonoco::class.java).build()
        repositorio = RepositorioMonoco(banco)
        runBlocking { repositorio.prepararCatalogo() }
    }

    @After fun fechar() { banco.close() }

    private suspend fun criarCampanha(): Long {
        repositorio.salvarPersonagem(Personagem(nome = "Aurel"))
        val idPersonagem = banco.monocoDao().observarPersonagens().first().single().id
        return repositorio.criarCampanha("Minha história", idPersonagem, 1)
    }

    @Test fun campanhaNasceComLivroPersonagemEAberturaSalva() = runBlocking {
        val id = criarCampanha()
        val campanha = banco.monocoDao().buscarCampanha(id)!!
        assertNotNull(banco.monocoDao().buscarPersonagem(campanha.personagemId))
        val livro = banco.monocoDao().buscarLivro(campanha.livroId)!!
        val mensagem = banco.monocoDao().observarMensagens(id, 30).first().single()
        assertEquals(livro.abertura, mensagem.texto)
        assertEquals(Mensagem.MESTRE, mensagem.autor)
    }

    @Test fun criacaoInvalidaNaoDeixaCampanhaParcial() = runBlocking {
        try {
            repositorio.criarCampanha("Inválida", 999, 1)
            fail("Deveria rejeitar um personagem inexistente")
        } catch (_: IllegalArgumentException) { }
        assertTrue(banco.monocoDao().observarCampanhas().first().isEmpty())
    }

    @Test fun exclusaoProtegeFichaVinculadaECampanhaRemoveSeusDependentes() = runBlocking {
        val id = criarCampanha()
        val personagemId = banco.monocoDao().buscarCampanha(id)!!.personagemId
        repositorio.salvarMemoria(Memoria(campanhaId = id, titulo = "Aliada", texto = "Lia ajuda o grupo"))
        try {
            repositorio.excluirPersonagem(personagemId)
            fail("Deveria preservar a ficha vinculada")
        } catch (_: IllegalStateException) { }
        assertNotNull(banco.monocoDao().buscarPersonagem(personagemId))
        banco.monocoDao().excluirCampanha(id)
        assertTrue(banco.monocoDao().observarMensagens(id, 30).first().isEmpty())
        assertTrue(banco.monocoDao().observarMemorias(id).first().isEmpty())
        repositorio.excluirPersonagem(personagemId)
        assertNull(banco.monocoDao().buscarPersonagem(personagemId))
    }

    @Test fun contextoUsaSomenteMemoriasDaCampanhaECamposPermitidos() = runBlocking {
        val id = criarCampanha()
        val personagem = banco.monocoDao().observarPersonagens().first().single()
        repositorio.salvarPersonagem(personagem.copy(segredos = "Segredo oculto", compartilharSegredos = false))
        repositorio.salvarMemoria(Memoria(campanhaId = id, titulo = "Aliada", texto = "Lia é uma aliada"))
        repositorio.salvarMemoria(Memoria(campanhaId = id, titulo = "Antiga", texto = "Ignorar", ativa = false))
        val outraCampanha = repositorio.criarCampanha("Outra aventura", personagem.id, 2)
        repositorio.salvarMemoria(Memoria(campanhaId = outraCampanha, titulo = "Outro mundo", texto = "Não incluir nesta campanha"))
        val contexto = repositorio.montarContexto(id)
        assertFalse(contexto.camposPersonagem.containsKey("Segredos"))
        assertEquals(listOf("Lia é uma aliada"), contexto.memorias)
    }
}
