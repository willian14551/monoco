package br.com.monoco.personagem

import br.com.monoco.campanha.*
import br.com.monoco.campanha.CampanhaRepositorio
import br.com.monoco.comum.*
import br.com.monoco.conta.*
import br.com.monoco.conta.ContaServico
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PersonagemServico(
    private val personagens: PersonagemRepositorio,
    private val campanhas: CampanhaRepositorio,
    private val contas: ContaServico,
) {
    private val registrador = LoggerFactory.getLogger(javaClass)

    fun buscar(usuarioId: Long, personagemId: Long): PersonagemSaida =
        (personagens.buscarDoUsuario(personagemId, usuarioId) ?: naoEncontrado("Personagem"))
            .paraResposta()

    fun listar(
        usuarioId: Long,
        busca: String,
        pagina: Int,
        tamanho: Int,
        ordenarPor: String,
        direcao: String,
    ) =
        personagens
            .findAll(
                criarFiltro(usuarioId, busca, "nome"),
                criarPaginacao(pagina, tamanho, ordenarPor, direcao, setOf("id", "nome", "classe")),
            )
            .paraPagina { it.paraResposta() }

    @Transactional
    fun salvar(usuarioId: Long, personagemId: Long?, dados: PersonagemEntrada): PersonagemSaida {
        val personagem =
            if (personagemId == null) Personagem(usuario = contas.buscar(usuarioId))
            else
                personagens.bloquearParaAlteracao(personagemId, usuarioId)
                    ?: naoEncontrado("Personagem")
        personagem.aplicar(dados)
        val personagemSalvo = personagens.save(personagem)
        registrador.info(
            "Personagem salvo: usuarioId={}, personagemId={}",
            usuarioId,
            personagemSalvo.id,
        )
        return personagemSalvo.paraResposta()
    }

    @Transactional
    fun excluir(usuarioId: Long, personagemId: Long) {
        val personagem =
            personagens.bloquearParaAlteracao(personagemId, usuarioId)
                ?: naoEncontrado("Personagem")
        if (campanhas.existeParaPersonagem(personagemId))
            conflito("Este personagem tem campanhas vinculadas. Exclua essas campanhas primeiro.")
        personagens.delete(personagem)
        registrador.info(
            "Personagem excluído: usuarioId={}, personagemId={}",
            usuarioId,
            personagemId,
        )
    }
}
