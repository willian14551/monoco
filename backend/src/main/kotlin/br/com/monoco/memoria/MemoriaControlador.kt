package br.com.monoco.memoria

import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@Validated
@RequestMapping("/campanhas/{campanhaId}/memorias")
class MemoriaControlador(private val memorias: MemoriaServico) {
    @GetMapping
    fun listar(
        @AuthenticationPrincipal usuarioId: Long,
        @PathVariable @Positive campanhaId: Long,
        @RequestParam(required = false) ativa: Boolean?,
        @RequestParam(defaultValue = "0") pagina: Int,
        @RequestParam(defaultValue = "50") tamanho: Int,
    ) = memorias.listar(usuarioId, campanhaId, ativa, pagina, tamanho)

    @GetMapping("/{memoriaId}")
    fun buscar(
        @AuthenticationPrincipal usuarioId: Long,
        @PathVariable @Positive campanhaId: Long,
        @PathVariable @Positive memoriaId: Long,
    ) = memorias.buscar(usuarioId, campanhaId, memoriaId)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun criar(
        @AuthenticationPrincipal usuarioId: Long,
        @PathVariable @Positive campanhaId: Long,
        @Valid @RequestBody dados: MemoriaEntrada,
    ) = memorias.salvar(usuarioId, campanhaId, null, dados)

    @PutMapping("/{memoriaId}")
    fun editar(
        @AuthenticationPrincipal usuarioId: Long,
        @PathVariable @Positive campanhaId: Long,
        @PathVariable @Positive memoriaId: Long,
        @Valid @RequestBody dados: MemoriaEntrada,
    ) = memorias.salvar(usuarioId, campanhaId, memoriaId, dados)

    @DeleteMapping("/{memoriaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun excluir(
        @AuthenticationPrincipal usuarioId: Long,
        @PathVariable @Positive campanhaId: Long,
        @PathVariable @Positive memoriaId: Long,
    ) = memorias.excluir(usuarioId, campanhaId, memoriaId)
}
