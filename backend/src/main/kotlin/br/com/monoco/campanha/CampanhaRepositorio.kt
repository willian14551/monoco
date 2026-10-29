package br.com.monoco.campanha

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.*

interface CampanhaRepositorio : JpaRepository<Campanha, Long>, JpaSpecificationExecutor<Campanha> {
    @Query(
        "select campanha from Campanha campanha where campanha.id = :campanhaId and campanha.usuario.id = :usuarioId"
    )
    fun buscarDoUsuario(campanhaId: Long, usuarioId: Long): Campanha?

    @Query(
        "select (count(campanha) > 0) from Campanha campanha where campanha.personagem.id = :personagemId"
    )
    fun existeParaPersonagem(personagemId: Long): Boolean

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        "select campanha from Campanha campanha where campanha.id = :campanhaId and campanha.usuario.id = :usuarioId"
    )
    fun bloquearParaAlteracao(campanhaId: Long, usuarioId: Long): Campanha?
}
