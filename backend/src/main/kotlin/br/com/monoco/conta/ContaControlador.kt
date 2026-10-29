package br.com.monoco.conta

import io.swagger.v3.oas.annotations.security.SecurityRequirements
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
class ContaControlador(private val contas: ContaServico) {
    @SecurityRequirements
    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    fun cadastrar(@Valid @RequestBody dados: Cadastro) = contas.cadastrar(dados)

    @SecurityRequirements
    @PostMapping("/sessoes")
    fun entrar(@Valid @RequestBody dados: Login) = contas.entrar(dados)

    @DeleteMapping("/sessoes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun sair(@AuthenticationPrincipal usuarioId: Long) = contas.sair(usuarioId)

    @GetMapping("/perfil")
    fun buscarPerfil(@AuthenticationPrincipal usuarioId: Long) =
        contas.buscar(usuarioId).paraResposta()

    @PutMapping("/perfil")
    fun editarPerfil(
        @AuthenticationPrincipal usuarioId: Long,
        @Valid @RequestBody dados: PerfilEntrada,
    ) = contas.editar(usuarioId, dados)

    @PutMapping("/perfil/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun alterarSenha(
        @AuthenticationPrincipal usuarioId: Long,
        @Valid @RequestBody dados: SenhaEntrada,
    ) = contas.alterarSenha(usuarioId, dados)
}
