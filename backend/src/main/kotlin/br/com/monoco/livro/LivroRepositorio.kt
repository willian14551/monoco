package br.com.monoco.livro

import org.springframework.data.jpa.repository.*

interface LivroRepositorio : JpaRepository<Livro, Long>, JpaSpecificationExecutor<Livro>
