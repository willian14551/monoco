package com.example.monoco.dominio

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface Narrador {
    fun narrar(contexto: ContextoNarracao): Flow<String>
}
class NarradorDemonstracao : Narrador {
    override fun narrar(contexto: ContextoNarracao): Flow<String> = flow {
        val nome = contexto.camposPersonagem["Nome"] ?: "Seu personagem"
        val acao = contexto.historico.lastOrNull { it.autor == "jogador" }?.texto.orEmpty()
        val reacao = when (contexto.tom) {
            "Sombrio" -> "O silêncio pesa enquanto o cenário parece observar cada movimento."
            "Leve" -> "O momento traz uma surpresa e uma oportunidade inesperada."
            else -> "Um novo detalhe no cenário chama sua atenção."
        }
        val memoria = contexto.memorias.firstOrNull()?.let { "\n\nFato ativo no contexto: $it" }.orEmpty()
        val complemento = if (contexto.tamanhoResposta == "Curta") "" else
            "\n\nVocê pode investigar o ambiente, conversar com alguém ou seguir por outro caminho."
        val detalhe = if (contexto.tamanhoResposta == "Longa")
            " O resultado da próxima decisão ainda está em aberto; descreva como pretende agir." else ""
        val texto = "$nome tenta: ${acao.take(140)}\n\n$reacao$complemento$detalhe$memoria\n\nQual é sua próxima ação?"
        for (trecho in texto.chunked(16)) {
            delay(35)
            emit(trecho)
        }
    }
}
