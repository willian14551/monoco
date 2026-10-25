package com.example.monoco.ui.telas.campanhas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.monoco.dados.local.*
import com.example.monoco.ui.componentes.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TelaCampanhas(nomeJogador: String, campanhas: List<Campanha>, personagens: List<Personagem>,
    livros: List<Livro>, aoNovaCampanha: () -> Unit, aoAbrirCampanha: (Long) -> Unit,
    aoExcluirCampanha: (Long) -> Unit) {
    var excluirId by rememberSaveable { mutableStateOf<Long?>(null) }
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { CabecalhoTela("Suas aventuras", "Bem-vindo, $nomeJogador. A próxima história começa com você.") }
        item { AvisoLocal() }
        item { Button(onClick = aoNovaCampanha, modifier = Modifier.fillMaxWidth()) { Text("+ Nova campanha") } }
        if (campanhas.isEmpty()) item {
            EstadoVazio("Sua primeira história está por vir", "Crie um personagem, escolha uma aventura e comece a explorar.",
                "Começar uma história", aoNovaCampanha)
        }
        items(campanhas, key = { it.id }) { campanha ->
            Card(onClick = { aoAbrirCampanha(campanha.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val livro = livros.find { it.id == campanha.livroId }
                    Etiqueta(livro?.genero ?: "Aventura")
                    Text(campanha.titulo, style = MaterialTheme.typography.titleLarge)
                    Text("${personagens.find { it.id == campanha.personagemId }?.nome.orEmpty()} · ${livro?.titulo.orEmpty()}",
                        style = MaterialTheme.typography.bodyMedium)
                    Text("Última atividade: ${formatarData(campanha.ultimaAtividade)}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { aoAbrirCampanha(campanha.id) }) { Text("Continuar aventura →") }
                        TextButton(onClick = { excluirId = campanha.id }) { Text("Excluir") }
                    }
                }
            }
        }
    }
    excluirId?.let { id ->
        ConfirmarExclusao("Excluir campanha?", "O histórico e as memórias desta campanha também serão apagados.",
            aoCancelar = { excluirId = null }, aoConfirmar = { aoExcluirCampanha(id); excluirId = null })
    }
}

fun formatarData(momento: Long): String =
    SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.forLanguageTag("pt-BR")).format(Date(momento))
