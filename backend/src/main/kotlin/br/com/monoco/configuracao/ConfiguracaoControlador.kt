package br.com.monoco.configuracao

import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/configuracao")
class ConfiguracaoControlador(private val configuracoes: ConfiguracaoServico) {
    @GetMapping
    fun buscar(@AuthenticationPrincipal usuarioId: Long) = configuracoes.buscar(usuarioId)

    @PatchMapping
    fun atualizar(
        @AuthenticationPrincipal usuarioId: Long,
        @Valid @RequestBody dados: ConfiguracaoEntrada,
    ) = configuracoes.atualizar(usuarioId, dados)
}
