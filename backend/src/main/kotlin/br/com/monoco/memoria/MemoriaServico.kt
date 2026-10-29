package br.com.monoco.memoria

import br.com.monoco.campanha.*
import br.com.monoco.campanha.CampanhaServico
import br.com.monoco.comum.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemoriaServico(
    private val memorias: MemoriaRepositorio,
    private val campanhas: CampanhaServico,
) {
    private val registrador = LoggerFactory.getLogger(javaClass)

    fun listar(
        usuarioId: Long,
        campanhaId: Long,
        ativa: Boolean?,
        pagina: Int,
        tamanho: Int,
    ): Pagina<MemoriaSaida> {
        campanhas.buscarDoUsuario(usuarioId, campanhaId)
        return memorias
            .findAll(
                criarFiltro(campanhaId = campanhaId, ativa = ativa),
                criarPaginacao(pagina, tamanho, "id", "asc", setOf("id")),
            )
            .paraPagina { it.paraResposta() }
    }

    fun buscar(usuarioId: Long, campanhaId: Long, memoriaId: Long): MemoriaSaida {
        campanhas.buscarDoUsuario(usuarioId, campanhaId)
        return buscarDaCampanha(campanhaId, memoriaId).paraResposta()
    }

    private fun buscarDaCampanha(campanhaId: Long, memoriaId: Long) =
        memorias.buscarDaCampanha(memoriaId, campanhaId) ?: naoEncontrado("Memória")

    @Transactional
    fun salvar(
        usuarioId: Long,
        campanhaId: Long,
        memoriaId: Long?,
        dados: MemoriaEntrada,
    ): MemoriaSaida {
        val campanha = campanhas.buscarDoUsuario(usuarioId, campanhaId)
        val memoria =
            if (memoriaId == null) Memoria(campanha = campanha)
            else buscarDaCampanha(campanhaId, memoriaId)
        memoria.titulo = dados.titulo.obrigatorio("Título").trim()
        memoria.texto = dados.texto.obrigatorio("Texto").trim()
        memoria.ativa = dados.ativa ?: true
        if (memoria.titulo.isBlank() || memoria.texto.isBlank())
            invalido("Informe o título e o texto da memória.")
        val memoriaSalva = memorias.save(memoria)
        registrador.info(
            "Memória salva: usuarioId={}, campanhaId={}, memoriaId={}",
            usuarioId,
            campanhaId,
            memoriaSalva.id,
        )
        return memoriaSalva.paraResposta()
    }

    @Transactional
    fun excluir(usuarioId: Long, campanhaId: Long, memoriaId: Long) {
        campanhas.buscarDoUsuario(usuarioId, campanhaId)
        memorias.delete(buscarDaCampanha(campanhaId, memoriaId))
        registrador.info(
            "Memória excluída: usuarioId={}, campanhaId={}, memoriaId={}",
            usuarioId,
            campanhaId,
            memoriaId,
        )
    }
}
