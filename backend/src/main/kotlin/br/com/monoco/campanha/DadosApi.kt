package br.com.monoco.campanha

import br.com.monoco.comum.*
import jakarta.validation.constraints.*

data class CampanhaEntrada(
    @field:NotBlank @field:Size(max = 150) val titulo: String? = null,
    @field:NotNull @field:Positive val personagemId: Long? = null,
    @field:NotNull @field:Positive val livroId: Long? = null,
)

data class CampanhaSaida(
    val id: Long,
    val titulo: String,
    val personagemId: Long,
    val livroId: Long,
    val ultimaAtividade: Long,
)

fun Campanha.paraResposta() = CampanhaSaida(id, titulo, personagem.id, livro.id, ultimaAtividade)

data class AcaoEntrada(
    @field:NotBlank @field:Size(max = 2000) val texto: String? = null,
    @field:NotBlank @field:Pattern(regexp = "[0-9a-fA-F-]{36}") val requisicaoId: String? = null,
)

data class MensagemSaida(
    val id: Long,
    val campanhaId: Long,
    val autor: String,
    val texto: String,
    val criadaEm: Long,
    val interrompida: Boolean,
)

fun Mensagem.paraResposta() = MensagemSaida(id, campanha.id, autor, texto, criadaEm, interrompida)

data class Historico(
    val mensagens: List<MensagemSaida>,
    val total: Long,
    val temAnteriores: Boolean,
)
