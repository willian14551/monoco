package br.com.monoco.personagem

import br.com.monoco.comum.*
import jakarta.validation.constraints.*

data class PersonagemEntrada(
    @field:NotBlank @field:Size(max = 100) val nome: String? = null,
    @field:Size(max = 100) val classe: String? = null,
    @field:Size(max = 5000) val atributos: String? = null,
    @field:Size(max = 10000) val historia: String? = null,
    @field:Size(max = 5000) val segredos: String? = null,
    val compartilharNome: Boolean? = null,
    val compartilharClasse: Boolean? = null,
    val compartilharAtributos: Boolean? = null,
    val compartilharHistoria: Boolean? = null,
    val compartilharSegredos: Boolean? = null,
)

data class PersonagemSaida(
    val id: Long,
    val nome: String,
    val classe: String,
    val atributos: String,
    val historia: String,
    val segredos: String,
    val compartilharNome: Boolean,
    val compartilharClasse: Boolean,
    val compartilharAtributos: Boolean,
    val compartilharHistoria: Boolean,
    val compartilharSegredos: Boolean,
)

fun Personagem.paraResposta() =
    PersonagemSaida(
        id,
        nome,
        classe,
        atributos,
        historia,
        segredos,
        compartilharNome,
        compartilharClasse,
        compartilharAtributos,
        compartilharHistoria,
        compartilharSegredos,
    )

fun Personagem.aplicar(dados: PersonagemEntrada) {
    nome = dados.nome.obrigatorio("Nome").trim()
    if (nome.isBlank()) invalido("O nome do personagem é obrigatório.")
    classe = dados.classe.orEmpty().trim()
    atributos = dados.atributos.orEmpty()
    historia = dados.historia.orEmpty()
    segredos = dados.segredos.orEmpty()
    compartilharNome = dados.compartilharNome ?: true
    compartilharClasse = dados.compartilharClasse ?: true
    compartilharAtributos = dados.compartilharAtributos ?: true
    compartilharHistoria = dados.compartilharHistoria ?: true
    compartilharSegredos = dados.compartilharSegredos ?: true
}
