package br.com.monoco.livro

import br.com.monoco.comum.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class LivroServico(private val livros: LivroRepositorio) {
    fun listar(
        busca: String,
        genero: String?,
        pagina: Int,
        tamanho: Int,
        ordenarPor: String,
        direcao: String,
    ) =
        livros
            .findAll(
                criarFiltro(busca = busca, genero = genero),
                criarPaginacao(
                    pagina,
                    tamanho,
                    ordenarPor,
                    direcao,
                    setOf("id", "titulo", "genero"),
                ),
            )
            .paraPagina { it.paraResposta() }
}
