package br.com.monoco.campanha

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.*

interface MensagemRepositorio : JpaRepository<Mensagem, Long> {
    @Query(
        "select mensagem from Mensagem mensagem where mensagem.campanha.id = :campanhaId order by mensagem.id desc"
    )
    fun listarRecentes(campanhaId: Long, paginacao: Pageable): List<Mensagem>

    @Query(
        "select mensagem from Mensagem mensagem where mensagem.campanha.id = :campanhaId and mensagem.id < :antesDe order by mensagem.id desc"
    )
    fun listarAnteriores(campanhaId: Long, antesDe: Long, paginacao: Pageable): List<Mensagem>

    @Query(
        "select mensagem from Mensagem mensagem where mensagem.campanha.id = :campanhaId and mensagem.requisicaoId = :requisicaoId"
    )
    fun buscarPorRequisicao(campanhaId: Long, requisicaoId: String): Mensagem?

    @Query("select count(mensagem) from Mensagem mensagem where mensagem.campanha.id = :campanhaId")
    fun contarDaCampanha(campanhaId: Long): Long
}
