package br.com.monoco.configuracao

import org.springframework.data.jpa.repository.*

interface ConfiguracaoRepositorio : JpaRepository<ConfiguracaoIa, Long> {
    @Query(
        "select configuracao from ConfiguracaoIa configuracao where configuracao.usuario.id = :usuarioId"
    )
    fun buscarPorUsuario(usuarioId: Long): ConfiguracaoIa?
}
