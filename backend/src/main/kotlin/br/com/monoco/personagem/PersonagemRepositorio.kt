package br.com.monoco.personagem

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.*

interface PersonagemRepositorio :
    JpaRepository<Personagem, Long>, JpaSpecificationExecutor<Personagem> {
    @Query(
        "select personagem from Personagem personagem where personagem.id = :personagemId and personagem.usuario.id = :usuarioId"
    )
    fun buscarDoUsuario(personagemId: Long, usuarioId: Long): Personagem?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        "select personagem from Personagem personagem where personagem.id = :personagemId and personagem.usuario.id = :usuarioId"
    )
    fun bloquearParaAlteracao(personagemId: Long, usuarioId: Long): Personagem?
}
