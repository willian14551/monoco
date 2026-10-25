package com.example.monoco.ui.telas.livros

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.monoco.dados.local.Livro
import com.example.monoco.ui.componentes.*

@Composable
fun TelaDetalheLivro(livro: Livro, aoVoltar: () -> Unit, aoUsarLivro: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        CabecalhoTela(livro.titulo, livro.descricao, aoVoltar)
        Etiqueta("${livro.genero} · ${livro.duracao}")
        Text("A aventura", style = MaterialTheme.typography.titleLarge)
        Text(livro.premissa)
        Text("Como jogar", style = MaterialTheme.typography.titleLarge)
        Text(livro.regras)
        Text("O primeiro capítulo", style = MaterialTheme.typography.titleLarge)
        OutlinedCard { Text(livro.abertura, Modifier.padding(20.dp)) }
        AvisoLocal("Aventura original de exemplo. Nesta versão, as respostas do chat são um roteiro de demonstração.")
        Button(onClick = aoUsarLivro, modifier = Modifier.fillMaxWidth()) { Text("Usar nesta campanha") }
    }
}
