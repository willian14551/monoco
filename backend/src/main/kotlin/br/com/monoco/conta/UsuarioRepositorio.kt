package br.com.monoco.conta

import org.springframework.data.jpa.repository.*

interface UsuarioRepositorio : JpaRepository<Usuario, Long> {
    @Query("select usuario from Usuario usuario where usuario.email = :email")
    fun buscarPorEmail(email: String): Usuario?

    @Query("select (count(usuario) > 0) from Usuario usuario where usuario.email = :email")
    fun existePorEmail(email: String): Boolean
}
