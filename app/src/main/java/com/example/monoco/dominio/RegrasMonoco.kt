package com.example.monoco.dominio

object RegrasMonoco {
    const val LIMITE_ACAO = 2000

    fun erroNome(nome: String): String? =
        if (nome.trim().length < 2) "Informe pelo menos 2 caracteres." else null

    fun erroAcao(acao: String): String? = when {
        acao.isBlank() -> "Descreva a ação do seu personagem."
        acao.length > LIMITE_ACAO -> "Use no máximo $LIMITE_ACAO caracteres."
        else -> null
    }

    fun erroMemoria(titulo: String, texto: String): String? = when {
        titulo.isBlank() -> "Informe o título da memória."
        texto.isBlank() -> "Descreva o que o mestre deve lembrar."
        else -> null
    }

    fun erroCriatividade(valor: Float): String? =
        if (!valor.isFinite() || valor !in 0f..1f) "A criatividade deve ficar entre 0 e 1." else null
}

data class CampoDoPersonagem(val nome: String, val valor: String, val acessivel: Boolean)
data class MemoriaDoContexto(val texto: String, val ativa: Boolean)
data class MensagemDoContexto(val autor: String, val texto: String)

data class ContextoNarracao(
    val regras: String,
    val camposPersonagem: Map<String, String>,
    val memorias: List<String>,
    val historico: List<MensagemDoContexto>,
    val tom: String,
    val tamanhoResposta: String,
    val criatividade: Float
)

object MontadorContexto {
    fun montar(
        regras: String,
        campos: List<CampoDoPersonagem>,
        memorias: List<MemoriaDoContexto>,
        historico: List<MensagemDoContexto>,
        tom: String,
        tamanhoResposta: String,
        criatividade: Float
    ) = ContextoNarracao(
        regras = regras,
        camposPersonagem = campos.filter { it.acessivel && it.valor.isNotBlank() }
            .associate { it.nome to it.valor },
        memorias = memorias.filter { it.ativa }.map { it.texto },
        historico = historico.takeLast(20),
        tom = tom,
        tamanhoResposta = tamanhoResposta,
        criatividade = criatividade
    )
}
