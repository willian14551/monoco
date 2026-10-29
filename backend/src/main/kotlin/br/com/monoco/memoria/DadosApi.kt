package br.com.monoco.memoria

import br.com.monoco.comum.*
import jakarta.validation.constraints.*

data class MemoriaEntrada(
    @field:NotBlank @field:Size(max = 120) val titulo: String? = null,
    @field:NotBlank @field:Size(max = 5000) val texto: String? = null,
    val ativa: Boolean? = null,
)

data class MemoriaSaida(
    val id: Long,
    val campanhaId: Long,
    val titulo: String,
    val texto: String,
    val ativa: Boolean,
)

fun Memoria.paraResposta() = MemoriaSaida(id, campanha.id, titulo, texto, ativa)
