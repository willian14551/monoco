package com.example.monoco.ui.previas

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.monoco.dados.livrosIniciais
import com.example.monoco.dados.local.Campanha
import com.example.monoco.dados.local.Personagem
import com.example.monoco.ui.telas.campanhas.TelaCampanhas
import com.example.monoco.ui.telas.livros.TelaLivros
import com.example.monoco.ui.telas.personagens.TelaEditarPersonagem
import com.example.monoco.ui.theme.MonocoTheme

private val fichaExemplo = Personagem(id = 1, nome = "Kael Voss", classe = "Explorador",
    historia = "Um viajante em busca de um mapa perdido.")

@Preview(name = "Campanhas · 360 dp", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun PreviaCampanhas() {
    MonocoTheme {
        TelaCampanhas(nomeJogador = "Aventureiro",
            campanhas = listOf(Campanha(id = 1, titulo = "O segredo de Salmar", personagemId = 1, livroId = 1)),
            personagens = listOf(fichaExemplo), livros = livrosIniciais,
            aoNovaCampanha = {}, aoAbrirCampanha = {}, aoExcluirCampanha = {})
    }
}

@Preview(name = "Livros · 360 dp", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun PreviaLivros() {
    MonocoTheme { TelaLivros(livrosIniciais, aoAbrirLivro = {}) }
}

@Preview(name = "Ficha · 360 dp", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun PreviaFicha() {
    MonocoTheme {
        TelaEditarPersonagem(fichaExemplo, salvando = false, aoVoltar = {}, aoSalvar = {})
    }
}
