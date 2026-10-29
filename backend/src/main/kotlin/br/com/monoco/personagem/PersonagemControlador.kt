package br.com.monoco.personagem

import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@Validated
@RequestMapping("/personagens")
class PersonagemControlador(private val personagens: PersonagemServico) {
    @GetMapping
    fun listar(
        @AuthenticationPrincipal usuarioId: Long,
        @RequestParam(defaultValue = "") busca: String,
        @RequestParam(defaultValue = "0") pagina: Int,
        @RequestParam(defaultValue = "50") tamanho: Int,
        @RequestParam(defaultValue = "nome") ordenarPor: String,
        @RequestParam(defaultValue = "asc") direcao: String,
    ) = personagens.listar(usuarioId, busca, pagina, tamanho, ordenarPor, direcao)

    @GetMapping("/{personagemId}")
    fun buscar(
        @AuthenticationPrincipal usuarioId: Long,
        @PathVariable @Positive personagemId: Long,
    ) = personagens.buscar(usuarioId, personagemId)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun criar(
        @AuthenticationPrincipal usuarioId: Long,
        @Valid @RequestBody dados: PersonagemEntrada,
    ) = personagens.salvar(usuarioId, null, dados)

    @PutMapping("/{personagemId}")
    fun editar(
        @AuthenticationPrincipal usuarioId: Long,
        @PathVariable @Positive personagemId: Long,
        @Valid @RequestBody dados: PersonagemEntrada,
    ) = personagens.salvar(usuarioId, personagemId, dados)

    @DeleteMapping("/{personagemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun excluir(
        @AuthenticationPrincipal usuarioId: Long,
        @PathVariable @Positive personagemId: Long,
    ) = personagens.excluir(usuarioId, personagemId)
}
