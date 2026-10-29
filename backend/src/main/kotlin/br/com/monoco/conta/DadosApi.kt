package br.com.monoco.conta

import br.com.monoco.comum.*
import jakarta.validation.constraints.*

data class Cadastro(
    @field:NotBlank @field:Size(min = 2, max = 100) val nome: String? = null,
    @field:NotBlank @field:Email @field:Size(max = 254) val email: String? = null,
    @field:NotBlank @field:Size(min = 8, max = 72) val senha: String? = null,
)

data class Login(
    @field:NotBlank @field:Email val email: String? = null,
    @field:NotBlank @field:Size(max = 72) val senha: String? = null,
)

data class PerfilEntrada(
    @field:NotBlank @field:Size(min = 2, max = 100) val nome: String? = null,
    @field:NotBlank @field:Email @field:Size(max = 254) val email: String? = null,
)

data class SenhaEntrada(
    @field:NotBlank @field:Size(max = 72) val senhaAtual: String? = null,
    @field:NotBlank @field:Size(min = 8, max = 72) val novaSenha: String? = null,
)

data class PerfilSaida(val id: Long, val nome: String, val email: String)

fun Usuario.paraResposta() = PerfilSaida(id, nome, email)

data class Sessao(val token: String, val expiraEm: Long, val usuario: PerfilSaida)
