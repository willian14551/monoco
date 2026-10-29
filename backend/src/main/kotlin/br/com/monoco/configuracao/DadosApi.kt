package br.com.monoco.configuracao

import br.com.monoco.comum.*
import jakarta.validation.constraints.*

data class ConfiguracaoSaida(
    val provedor: String,
    val modelo: String,
    val chaveConfigurada: Boolean,
    val finalChave: String?,
    val tom: String,
    val tamanhoResposta: String,
    val criatividade: Double,
    val demonstracaoPermitida: Boolean,
)

fun ConfiguracaoIa.paraResposta(demonstracaoPermitida: Boolean) =
    ConfiguracaoSaida(
        provedor,
        modelo,
        chaveCifrada != null,
        finalChave,
        tom,
        tamanhoResposta,
        criatividade,
        demonstracaoPermitida,
    )

data class ConfiguracaoEntrada(
    @field:Pattern(regexp = "OpenAI|OpenRouter|Demonstracao") val provedor: String? = null,
    @field:Size(max = 150) val modelo: String? = null,
    @field:Size(min = 1, max = 512) val chave: String? = null,
    val removerChave: Boolean? = null,
    @field:Pattern(regexp = "Aventura|Sombrio|Leve") val tom: String? = null,
    @field:Pattern(regexp = "Curta|Média|Longa") val tamanhoResposta: String? = null,
    @field:DecimalMin("0.0") @field:DecimalMax("1.0") val criatividade: Double? = null,
)
