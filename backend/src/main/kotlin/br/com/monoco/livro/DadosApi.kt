package br.com.monoco.livro

import br.com.monoco.comum.*
import jakarta.validation.constraints.*

data class LivroSaida(
    val id: Long,
    val titulo: String,
    val genero: String,
    val descricao: String,
    val premissa: String,
    val regras: String,
    val abertura: String,
    val duracao: String,
)

fun Livro.paraResposta() =
    LivroSaida(id, titulo, genero, descricao, premissa, regras, abertura, duracao)
