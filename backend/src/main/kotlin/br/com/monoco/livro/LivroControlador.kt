package br.com.monoco.livro

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/livros")
class LivroControlador(private val livros: LivroServico) {
    @GetMapping
    fun listar(
        @RequestParam(defaultValue = "") busca: String,
        @RequestParam(required = false) genero: String?,
        @RequestParam(defaultValue = "0") pagina: Int,
        @RequestParam(defaultValue = "50") tamanho: Int,
        @RequestParam(defaultValue = "titulo") ordenarPor: String,
        @RequestParam(defaultValue = "asc") direcao: String,
    ) = livros.listar(busca, genero, pagina, tamanho, ordenarPor, direcao)
}
