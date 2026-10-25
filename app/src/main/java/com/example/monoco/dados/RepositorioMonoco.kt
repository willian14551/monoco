package com.example.monoco.dados

import androidx.room.withTransaction
import com.example.monoco.dados.local.*
import com.example.monoco.dominio.*
import kotlinx.coroutines.flow.first

class RepositorioMonoco(private val banco: BancoMonoco) {
    val dao = banco.monocoDao()

    suspend fun prepararCatalogo() = dao.inserirLivros(livrosIniciais)

    suspend fun salvarPersonagem(personagem: Personagem) {
        require(personagem.nome.isNotBlank()) { "O nome do personagem é obrigatório." }
        if (personagem.id != 0L) requireNotNull(dao.buscarPersonagem(personagem.id)) {
            "Personagem não encontrado."
        }
        dao.salvarPersonagem(personagem.copy(nome = personagem.nome.trim()))
    }

    suspend fun excluirPersonagem(id: Long) = banco.withTransaction {
        check(dao.contarCampanhasDoPersonagem(id) == 0) {
            "Este personagem tem campanhas vinculadas. Exclua essas campanhas primeiro."
        }
        dao.excluirPersonagem(id)
    }

    suspend fun criarCampanha(titulo: String, personagemId: Long, livroId: Long): Long =
        banco.withTransaction {
            require(titulo.isNotBlank()) { "Informe o título da campanha." }
            requireNotNull(dao.buscarPersonagem(personagemId)) { "Escolha um personagem válido." }
            val livro = requireNotNull(dao.buscarLivro(livroId)) { "Escolha um livro válido." }
            val id = dao.inserirCampanha(Campanha(titulo = titulo.trim(),
                personagemId = personagemId, livroId = livroId))
            dao.inserirMensagem(Mensagem(campanhaId = id, autor = Mensagem.MESTRE, texto = livro.abertura))
            id
        }

    suspend fun salvarMensagem(campanhaId: Long, autor: String, texto: String,
        interrompida: Boolean = false) = banco.withTransaction {
        requireNotNull(dao.buscarCampanha(campanhaId)) { "Campanha não encontrada." }
        dao.inserirMensagem(Mensagem(campanhaId = campanhaId, autor = autor,
            texto = texto, interrompida = interrompida))
        dao.atualizarAtividade(campanhaId, System.currentTimeMillis())
    }

    suspend fun salvarMemoria(memoria: Memoria) {
        require(RegrasMonoco.erroMemoria(memoria.titulo, memoria.texto) == null) {
            RegrasMonoco.erroMemoria(memoria.titulo, memoria.texto).orEmpty()
        }
        requireNotNull(dao.buscarCampanha(memoria.campanhaId)) { "Campanha não encontrada." }
        if (memoria.id != 0L) {
            val original = requireNotNull(dao.buscarMemoria(memoria.id)) { "Memória não encontrada." }
            require(original.campanhaId == memoria.campanhaId) { "A memória pertence a outra campanha." }
        }
        dao.salvarMemoria(memoria.copy(titulo = memoria.titulo.trim(), texto = memoria.texto.trim()))
    }

    suspend fun montarContexto(campanhaId: Long): ContextoNarracao {
        val campanha = requireNotNull(dao.buscarCampanha(campanhaId))
        val personagem = requireNotNull(dao.buscarPersonagem(campanha.personagemId))
        val livro = requireNotNull(dao.buscarLivro(campanha.livroId))
        val preferencias = dao.observarPreferencias().first() ?: Preferencias()
        return MontadorContexto.montar(
            regras = livro.regras,
            campos = listOf(
                CampoDoPersonagem("Nome", personagem.nome, personagem.compartilharNome),
                CampoDoPersonagem("Classe", personagem.classe, personagem.compartilharClasse),
                CampoDoPersonagem("Atributos", personagem.atributos, personagem.compartilharAtributos),
                CampoDoPersonagem("História", personagem.historia, personagem.compartilharHistoria),
                CampoDoPersonagem("Segredos", personagem.segredos, personagem.compartilharSegredos)
            ),
            memorias = dao.buscarMemoriasAtivas(campanhaId).map { MemoriaDoContexto(it.texto, it.ativa) },
            historico = dao.buscarMensagensRecentes(campanhaId, 20).reversed()
                .map { MensagemDoContexto(it.autor, it.texto) },
            tom = preferencias.tom, tamanhoResposta = preferencias.tamanhoResposta,
            criatividade = preferencias.criatividade
        )
    }
}
