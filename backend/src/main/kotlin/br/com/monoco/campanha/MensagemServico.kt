package br.com.monoco.campanha

import br.com.monoco.comum.*
import br.com.monoco.comum.invalido
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MensagemServico(
    private val campanhas: CampanhaServico,
    private val mensagens: MensagemRepositorio,
) {
    fun listar(usuarioId: Long, campanhaId: Long, antesDe: Long?, limite: Int): Historico {
        campanhas.buscarDoUsuario(usuarioId, campanhaId)
        if (limite !in 1..100 || (antesDe != null && antesDe <= 0))
            invalido("Parâmetros de histórico inválidos.")
        val pagina = PageRequest.of(0, limite + 1)
        val resultados =
            if (antesDe == null) mensagens.listarRecentes(campanhaId, pagina)
            else mensagens.listarAnteriores(campanhaId, antesDe, pagina)
        return Historico(
            resultados.take(limite).reversed().map { it.paraResposta() },
            mensagens.contarDaCampanha(campanhaId),
            resultados.size > limite,
        )
    }
}
