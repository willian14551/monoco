package com.example.monoco.ui.telas.livros

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.monoco.dados.local.Livro
import com.example.monoco.ui.componentes.*

@Composable
fun TelaLivros(livros: List<Livro>, aoAbrirLivro: (Long) -> Unit) {
    var busca by rememberSaveable { mutableStateOf("") }
    var genero by rememberSaveable { mutableStateOf("Todos") }
    val filtrados = livros.filter {
        (genero == "Todos" || it.genero == genero) &&
            (it.titulo.contains(busca.trim(), ignoreCase = true) || it.genero.contains(busca.trim(), ignoreCase = true))
    }
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { CabecalhoTela("Escolha seu mundo", "Aventuras prontas para começar, com cenário, personagens e um primeiro mistério.") }
        item { OutlinedTextField(value = busca, onValueChange = { busca = it },
            label = { Text("Buscar por nome ou gênero") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item { EscolhaEmChips(listOf("Todos") + livros.map { it.genero }.distinct(), genero) { genero = it } }
        if (filtrados.isEmpty()) item { EstadoVazio("Nenhum livro encontrado", "Tente outro nome ou gênero.") }
        items(filtrados, key = { it.id }) { livro ->
            Card(onClick = { aoAbrirLivro(livro.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Etiqueta(livro.genero)
                    Text(livro.titulo, style = MaterialTheme.typography.headlineSmall)
                    Text(livro.descricao)
                    Text(livro.duracao, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary)
                    TextButton(onClick = { aoAbrirLivro(livro.id) }) { Text("Conhecer aventura →") }
                }
            }
        }
    }
}
