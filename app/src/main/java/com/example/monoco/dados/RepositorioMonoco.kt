package com.example.monoco.dados

import androidx.room.withTransaction
import com.example.monoco.dados.remoto.ApiMonoco
import com.example.monoco.dados.local.*
import com.example.monoco.dominio.*
import kotlinx.coroutines.flow.first

class RepositorioMonoco(private val banco: BancoMonoco, val api: ApiMonoco? = null) {
    val dao = banco.monocoDao()

    suspend fun prepararCatalogo() {
        if(api == null) dao.inserirLivros(livrosIniciais) else sincronizar()
    }

    suspend fun sincronizar() {
        val remoto=api ?: return
        val personagens=remoto.personagens()
        val livros=remoto.livros()
        val campanhas=remoto.campanhas()
        val perfil=remoto.perfil()
        val config=remoto.configuracao()
        banco.withTransaction {
            dao.observarCampanhas().first().filter { local -> campanhas.none { it.id==local.id } }
                .forEach { dao.excluirCampanha(it.id) }
            dao.atualizarPersonagens(personagens)
            dao.atualizarLivros(livros)
            dao.atualizarCampanhas(campanhas)
            dao.observarPersonagens().first().filter { local -> personagens.none { it.id==local.id } }
                .forEach { dao.excluirPersonagem(it.id) }
            dao.salvarPerfil(PerfilLocal(nome=perfil.getString("nome")))
            dao.salvarPreferencias(Preferencias(tom=config.getString("tom"),tamanhoResposta=config.getString("tamanhoResposta"),
                criatividade=config.getDouble("criatividade").toFloat(),provedorPlanejado=config.getString("provedor"),modeloPlanejado=config.getString("modelo")))
        }
    }

    suspend fun carregarCampanha(cid: Long,antesDe: Long?=null): Int {
        val remoto=api ?: return 0
        val historico=remoto.historico(cid,antesDe)
        val memorias=remoto.memorias(cid)
        banco.withTransaction {
            dao.atualizarMensagens(historico.mensagens)
            dao.limparMemorias(cid)
            dao.atualizarMemorias(memorias)
        }
        return historico.total
    }
    suspend fun excluirCampanha(id: Long) {
        api?.excluirCampanha(id)
        dao.excluirCampanha(id)
    }
    suspend fun excluirMemoria(id: Long) {
        val obj=dao.buscarMemoria(id) ?: return
        api?.excluirMemoria(obj.campanhaId,id)
        dao.excluirMemoria(id)
    }
    suspend fun salvarPerfil(nome: String,email: String) {
        api?.salvarPerfil(nome,email)
        dao.salvarPerfil(PerfilLocal(nome=nome.trim()))
    }
    suspend fun salvarPreferencias(d: Preferencias) {
        api?.salvarPreferencias(d)
        dao.salvarPreferencias(d)
    }
    suspend fun enviarAcaoOnline(cid: Long,texto: String,requisicaoId: String) {
        val mensagem=requireNotNull(api).enviarAcao(cid,texto,requisicaoId)
        dao.atualizarMensagens(listOf(mensagem))
    }
    suspend fun narrarOnline(cid: Long,aoTrecho: (String)->Unit) {
        val mensagem=requireNotNull(api).narrar(cid,aoTrecho)
        dao.atualizarMensagens(listOf(mensagem))
        dao.atualizarAtividade(cid,mensagem.criadaEm)
    }

    suspend fun salvarPersonagem(personagem: Personagem) {
        api?.let { it.salvarPersonagem(personagem); sincronizar(); return }
        require(personagem.nome.isNotBlank()) { "O nome do personagem é obrigatório." }
        if (personagem.id != 0L) requireNotNull(dao.buscarPersonagem(personagem.id)) {
            "Personagem não encontrado."
        }
        dao.salvarPersonagem(personagem.copy(nome = personagem.nome.trim()))
    }

    suspend fun excluirPersonagem(id: Long) {
        api?.let { it.excluirPersonagem(id); sincronizar(); return }
        banco.withTransaction {
        check(dao.contarCampanhasDoPersonagem(id) == 0) {
            "Este personagem tem campanhas vinculadas. Exclua essas campanhas primeiro."
        }
        dao.excluirPersonagem(id)
        }
    }

    suspend fun criarCampanha(titulo: String, personagemId: Long, livroId: Long): Long {
        api?.let {
            val id=it.criarCampanha(titulo,personagemId,livroId)
            sincronizar(); carregarCampanha(id)
            return id
        }
        return banco.withTransaction {
            require(titulo.isNotBlank()) { "Informe o título da campanha." }
            requireNotNull(dao.buscarPersonagem(personagemId)) { "Escolha um personagem válido." }
            val livro = requireNotNull(dao.buscarLivro(livroId)) { "Escolha um livro válido." }
            val id = dao.inserirCampanha(Campanha(titulo = titulo.trim(),
                personagemId = personagemId, livroId = livroId))
            dao.inserirMensagem(Mensagem(campanhaId = id, autor = Mensagem.MESTRE, texto = livro.abertura))
            id
        }
    }

    suspend fun salvarMensagem(campanhaId: Long, autor: String, texto: String,
        interrompida: Boolean = false) = banco.withTransaction {
        requireNotNull(dao.buscarCampanha(campanhaId)) { "Campanha não encontrada." }
        dao.inserirMensagem(Mensagem(campanhaId = campanhaId, autor = autor,
            texto = texto, interrompida = interrompida))
        dao.atualizarAtividade(campanhaId, System.currentTimeMillis())
    }

    suspend fun salvarMemoria(memoria: Memoria) {
        api?.let { it.salvarMemoria(memoria); carregarCampanha(memoria.campanhaId); return }
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
