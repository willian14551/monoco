package br.com.monoco.memoria

import org.springframework.data.jpa.repository.*

interface MemoriaRepositorio : JpaRepository<Memoria, Long>, JpaSpecificationExecutor<Memoria> {
    @Query(
        "select memoria from Memoria memoria where memoria.id = :memoriaId and memoria.campanha.id = :campanhaId"
    )
    fun buscarDaCampanha(memoriaId: Long, campanhaId: Long): Memoria?

    @Query(
        "select memoria from Memoria memoria where memoria.campanha.id = :campanhaId and memoria.ativa = true order by memoria.id"
    )
    fun listarAtivas(campanhaId: Long): List<Memoria>
}
