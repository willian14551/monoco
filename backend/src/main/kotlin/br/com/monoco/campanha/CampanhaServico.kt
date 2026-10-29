package br.com.monoco.campanha

import br.com.monoco.comum.*
import br.com.monoco.conta.*
import br.com.monoco.conta.ContaServico
import br.com.monoco.livro.*
import br.com.monoco.livro.LivroRepositorio
import br.com.monoco.personagem.*
import br.com.monoco.personagem.PersonagemRepositorio
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CampanhaServico(
    private val campanhas: CampanhaRepositorio,
    private val personagens: PersonagemRepositorio,
    private val livros: LivroRepositorio,
    private val mensagens: MensagemRepositorio,
    private val contas: ContaServico,
) {
    private val registrador = LoggerFactory.getLogger(javaClass)

    fun buscarDoUsuario(usuarioId: Long, campanhaId: Long) =
        campanhas.buscarDoUsuario(campanhaId, usuarioId) ?: naoEncontrado("Campanha")

    fun listar(
        usuarioId: Long,
        busca: String,
        pagina: Int,
        tamanho: Int,
        ordenarPor: String,
        direcao: String,
    ) =
        campanhas
            .findAll(
                criarFiltro(usuarioId, busca),
                criarPaginacao(
                    pagina,
                    tamanho,
                    ordenarPor,
                    direcao,
                    setOf("id", "titulo", "ultimaAtividade"),
                ),
            )
            .paraPagina { it.paraResposta() }

    @Transactional
    fun criar(usuarioId: Long, dados: CampanhaEntrada): CampanhaSaida {
        val personagem =
            personagens.bloquearParaAlteracao(
                dados.personagemId.obrigatorio("Personagem"),
                usuarioId,
            ) ?: naoEncontrado("Personagem")
        val livro =
            livros.findById(dados.livroId.obrigatorio("Livro")).orElse(null)
                ?: naoEncontrado("Livro")
        val campanha =
            campanhas.save(
                Campanha(
                    usuario = contas.buscar(usuarioId),
                    personagem = personagem,
                    livro = livro,
                    titulo = dados.titulo.obrigatorio("Título").trim(),
                )
            )
        mensagens.save(Mensagem(campanha = campanha, autor = "mestre", texto = livro.abertura))
        registrador.info("Campanha criada: usuarioId={}, campanhaId={}", usuarioId, campanha.id)
        return campanha.paraResposta()
    }

    @Transactional
    fun excluir(usuarioId: Long, campanhaId: Long) {
        val campanha =
            campanhas.bloquearParaAlteracao(campanhaId, usuarioId) ?: naoEncontrado("Campanha")
        if (
            campanha.geracaoId != null &&
                System.currentTimeMillis() - (campanha.geracaoInicio ?: 0) < 120_000
        )
            conflito("Aguarde a narração terminar antes de excluir a campanha.")
        campanhas.delete(campanha)
        registrador.info("Campanha excluída: usuarioId={}, campanhaId={}", usuarioId, campanhaId)
    }
}
