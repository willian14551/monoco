package com.example.monoco.ui

import com.example.monoco.dados.remoto.ConfiguracaoOnline
import java.util.UUID
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.monoco.dados.RepositorioMonoco
import com.example.monoco.dados.local.*
import com.example.monoco.dominio.Narrador
import com.example.monoco.dominio.RegrasMonoco
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DadosLocais(
    val personagens: List<Personagem> = emptyList(),
    val livros: List<Livro> = emptyList(),
    val campanhas: List<Campanha> = emptyList(),
    val perfil: PerfilLocal = PerfilLocal(),
    val preferencias: Preferencias = Preferencias()
)

data class DadosCampanha(
    val id: Long = 0,
    val mensagens: List<Mensagem> = emptyList(),
    val memorias: List<Memoria> = emptyList(),
    val totalMensagens: Int = 0
)

data class EstadoOperacao(
    val carregando: Boolean = true,
    val salvando: Boolean = false,
    val campanhaEmNarracao: Long? = null,
    val trechoNarracao: String = ""
)

data class EstadoMonoco(
    val dados: DadosLocais = DadosLocais(),
    val campanha: DadosCampanha = DadosCampanha(),
    val operacao: EstadoOperacao = EstadoOperacao()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MonocoViewModel @Inject constructor(
    private val repositorio: RepositorioMonoco,
    private val narrador: Narrador
) : ViewModel() {
    private val dao = repositorio.dao
    val emailOnline = MutableStateFlow("")
    val configuracaoOnline = MutableStateFlow(ConfiguracaoOnline())
    private val totaisServidor = MutableStateFlow<Map<Long,Int>>(emptyMap())
    private val requisicoesPendentes = mutableMapOf<Pair<Long,String>,String>()
    suspend fun atualizarContaOnline() {
        val api=repositorio.api ?: return
        emailOnline.value=api.perfil().getString("email")
        val c=api.configuracao()
        configuracaoOnline.value=ConfiguracaoOnline(c.getString("provedor"),c.getString("modelo"),c.getBoolean("chaveConfigurada"),
            if(c.isNull("finalChave")) "" else c.getString("finalChave"),c.getBoolean("demonstracaoPermitida"))
    }
    private suspend fun atualizarCampanhaOnline(id: Long,antesDe: Long?=null) {
        if(repositorio.api==null) return
        val total=repositorio.carregarCampanha(id,antesDe)
        totaisServidor.update { it + (id to total) }
    }
    private val campanhaSelecionada = MutableStateFlow(0L)
    private val limiteMensagens = MutableStateFlow(30)
    private val operacao = MutableStateFlow(EstadoOperacao())
    private val avisos = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val mensagensDeAviso = avisos.asSharedFlow()
    private var tarefaNarracao: Job? = null

    private val dadosLocais = combine(
        dao.observarPersonagens(), dao.observarLivros(), dao.observarCampanhas(),
        dao.observarPerfil(), dao.observarPreferencias()
    ) { personagens, livros, campanhas, perfil, preferencias ->
        DadosLocais(personagens, livros, campanhas, perfil ?: PerfilLocal(), preferencias ?: Preferencias())
    }

    private val dadosCampanha = combine(campanhaSelecionada, limiteMensagens) { id, limite -> id to limite }
        .flatMapLatest { (id, limite) ->
            combine(dao.observarMensagens(id, limite), dao.observarMemorias(id),
                dao.observarTotalMensagens(id), totaisServidor) { mensagens, memorias, total, totais ->
                DadosCampanha(id, mensagens, memorias, maxOf(total,totais[id] ?: 0))
            }
        }

    val estado = combine(dadosLocais, dadosCampanha, operacao) { dados, campanha, tarefa ->
        EstadoMonoco(dados, campanha, tarefa)
    }.catch {
        avisos.emit("Não foi possível ler os dados locais. Feche e abra o aplicativo para tentar novamente.")
        emit(EstadoMonoco(operacao = EstadoOperacao(carregando = false)))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoMonoco())

    init {
        viewModelScope.launch {
            try {
                repositorio.prepararCatalogo()
                atualizarContaOnline()
            } catch (erro: Exception) {
                if (erro is CancellationException) throw erro
                avisos.emit("Não foi possível carregar as aventuras. Confira a conexão e use Atualizar para tentar novamente.")
            } finally {
                operacao.update { it.copy(carregando = false) }
            }
        }
    }

    fun selecionarCampanha(id: Long) {
        if (id != campanhaSelecionada.value) {
            limiteMensagens.value = 30
            campanhaSelecionada.value = id
        }
        if(repositorio.api!=null) viewModelScope.launch {
            try { atualizarCampanhaOnline(id) }
            catch(e: Exception) { if(e is CancellationException) throw e; avisos.emit(e.message ?: "Não foi possível carregar a campanha.") }
        }
    }

    fun carregarMensagensAnteriores() {
        executarAlteracao {
            val id=campanhaSelecionada.value
            val antiga=dao.buscarMensagensRecentes(id,limiteMensagens.value).lastOrNull()?.id
            if(antiga!=null) atualizarCampanhaOnline(id,antiga)
            limiteMensagens.update { it + 30 }
        }
    }
    fun atualizarDados() = executarAlteracao { repositorio.sincronizar(); atualizarContaOnline();
        if(campanhaSelecionada.value!=0L) atualizarCampanhaOnline(campanhaSelecionada.value) }

    fun avisar(mensagem: String) { avisos.tryEmit(mensagem) }

    private fun executarAlteracao(acao: suspend () -> Unit) {
        if (operacao.value.salvando) return
        operacao.update { it.copy(salvando = true) }
        viewModelScope.launch {
            try {
                acao()
            } catch (erro: Exception) {
                if (erro is CancellationException) throw erro
                avisos.emit(erro.message ?: "Não foi possível salvar. Seus campos foram preservados.")
            } finally {
                operacao.update { it.copy(salvando = false) }
            }
        }
    }

    fun salvarPersonagem(personagem: Personagem, aoConcluir: () -> Unit) = executarAlteracao {
        repositorio.salvarPersonagem(personagem)
        avisos.emit("Personagem salvo.")
        aoConcluir()
    }

    fun excluirPersonagem(id: Long) = executarAlteracao {
        repositorio.excluirPersonagem(id)
        avisos.emit("Personagem excluído.")
    }

    fun criarCampanha(titulo: String, personagemId: Long, livroId: Long, aoConcluir: (Long) -> Unit) =
        executarAlteracao {
            val id = repositorio.criarCampanha(titulo, personagemId, livroId)
            aoConcluir(id)
        }

    fun excluirCampanha(id: Long) = executarAlteracao {
        check(operacao.value.campanhaEmNarracao != id) { "Pare a narração antes de excluir a campanha." }
        repositorio.excluirCampanha(id)
        avisos.emit("Campanha, mensagens e memórias excluídas.")
    }

    fun salvarMemoria(memoria: Memoria, aoConcluir: () -> Unit) = executarAlteracao {
        repositorio.salvarMemoria(memoria)
        avisos.emit("Memória salva.")
        aoConcluir()
    }

    fun alternarMemoria(memoria: Memoria) = executarAlteracao {
        repositorio.salvarMemoria(memoria.copy(ativa = !memoria.ativa))
    }

    fun excluirMemoria(id: Long) = executarAlteracao { repositorio.excluirMemoria(id) }

    fun salvarPerfil(nome: String) = executarAlteracao {
        require(RegrasMonoco.erroNome(nome) == null) { "Informe um nome com pelo menos 2 caracteres." }
        repositorio.salvarPerfil(nome,emailOnline.value)
        avisos.emit("Perfil atualizado.")
    }

    fun salvarPreferencias(preferencias: Preferencias) = executarAlteracao {
        require(RegrasMonoco.erroCriatividade(preferencias.criatividade) == null) {
            RegrasMonoco.erroCriatividade(preferencias.criatividade).orEmpty()
        }
        repositorio.salvarPreferencias(preferencias)
        avisos.emit("Preferências salvas.")
    }

    fun enviarAcao(campanhaId: Long, texto: String, aoSalvarAcao: () -> Unit) {
        val erro = RegrasMonoco.erroAcao(texto)
        if (erro != null) { avisar(erro); return }
        if(repositorio.api!=null) iniciarNarracaoOnline(campanhaId,texto.trim(),aoSalvarAcao)
        else iniciarNarracao(campanhaId, texto.trim(), aoSalvarAcao)
    }

    fun tentarNarracaoNovamente(campanhaId: Long) {
        if(repositorio.api!=null) iniciarNarracaoOnline(campanhaId,null) {}
        else iniciarNarracao(campanhaId,null) {}
    }

    fun salvarPerfilOnline(nome: String,email: String) = executarAlteracao {
        repositorio.salvarPerfil(nome,email); atualizarContaOnline(); avisos.emit("Perfil atualizado.")
    }
    fun alterarSenhaOnline(atual: String,nova: String) = executarAlteracao {
        repositorio.api?.alterarSenha(atual,nova)
    }
    fun configurarProvedorOnline(provedor: String,modelo: String,chave: String) = executarAlteracao {
        repositorio.api?.configurarProvedor(provedor,modelo,chave)
        repositorio.sincronizar(); atualizarContaOnline(); avisos.emit("Configuração validada e salva.")
    }
    fun removerChaveOnline() = executarAlteracao {
        repositorio.api?.removerChave(); atualizarContaOnline(); avisos.emit("Chave removida.")
    }
    private fun iniciarNarracaoOnline(cid: Long,texto: String?,aoSalvar: ()->Unit) {
        if(operacao.value.campanhaEmNarracao!=null || operacao.value.salvando) return
        operacao.update { it.copy(campanhaEmNarracao=cid,trechoNarracao="") }
        tarefaNarracao=viewModelScope.launch {
            try {
                if(texto!=null) {
                    val par=cid to texto
                    val requisicao=requisicoesPendentes.getOrPut(par) { UUID.randomUUID().toString() }
                    repositorio.enviarAcaoOnline(cid,texto,requisicao)
                    requisicoesPendentes.remove(par)
                    aoSalvar()
                }
                atualizarCampanhaOnline(cid)
                val recebido=StringBuilder()
                repositorio.narrarOnline(cid) { trecho ->
                    recebido.append(trecho)
                    operacao.update { it.copy(trechoNarracao=recebido.toString()) }
                }
                atualizarCampanhaOnline(cid)
            } catch(e: Exception) {
                if(e is CancellationException) throw e
                avisos.emit(e.message ?: "Falha na narração. Sua ação continua salva; tente novamente.")
            } finally {
                operacao.update { it.copy(campanhaEmNarracao=null,trechoNarracao="") }
            }
        }
    }

    private fun iniciarNarracao(campanhaId: Long, novaAcao: String?, aoSalvarAcao: () -> Unit) {
        if (operacao.value.campanhaEmNarracao != null || operacao.value.salvando) return
        operacao.update { it.copy(campanhaEmNarracao = campanhaId, trechoNarracao = "") }
        tarefaNarracao = viewModelScope.launch {
            val textoRecebido = StringBuilder()
            var narracaoGravada = false
            try {
                val ultima = dao.buscarMensagensRecentes(campanhaId, 1).firstOrNull()
                if (novaAcao != null) {
                    check(ultima?.autor != Mensagem.JOGADOR) { "Gere a resposta da ação pendente antes de enviar outra." }

                    repositorio.salvarMensagem(campanhaId, Mensagem.JOGADOR, novaAcao)
                    aoSalvarAcao()
                } else {
                    check(ultima?.autor == Mensagem.JOGADOR) { "Não há uma ação aguardando resposta." }
                }
                val contexto = repositorio.montarContexto(campanhaId)
                narrador.narrar(contexto).collect { trecho ->
                    textoRecebido.append(trecho)
                    operacao.update { it.copy(trechoNarracao = textoRecebido.toString()) }
                }
                withContext(NonCancellable) {
                    repositorio.salvarMensagem(campanhaId, Mensagem.MESTRE, textoRecebido.toString())
                    narracaoGravada = true
                }
            } catch (cancelamento: CancellationException) {
                if (!narracaoGravada && textoRecebido.isNotEmpty()) withContext(NonCancellable) {
                    try {
                        repositorio.salvarMensagem(campanhaId, Mensagem.MESTRE,
                            textoRecebido.toString(), interrompida = true)
                    } catch (_: Exception) {
                        avisos.tryEmit("Não foi possível guardar o trecho interrompido. A ação continua no histórico.")
                    }
                }
                throw cancelamento
            } catch (erro: Exception) {
                avisos.emit(erro.message ?: "Falha na narração. Sua ação continua salva; tente novamente.")
            } finally {
                operacao.update { it.copy(campanhaEmNarracao = null, trechoNarracao = "") }
            }
        }
    }

    fun pararNarracao() { tarefaNarracao?.cancel() }
}
