package com.example.monoco.ui.telas.campanhas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.monoco.dados.local.Livro
import com.example.monoco.dados.local.Personagem
import com.example.monoco.ui.componentes.*

@Composable
fun TelaNovaCampanha(personagens: List<Personagem>, livros: List<Livro>, livroInicial: Long,
    salvando: Boolean, aoVoltar: () -> Unit, aoCriarPersonagem: () -> Unit,
    aoIniciar: (String, Long, Long) -> Unit) {
    var titulo by rememberSaveable { mutableStateOf("") }
    var personagemId by rememberSaveable { mutableStateOf(0L) }
    var livroId by rememberSaveable { mutableStateOf(livroInicial) }
    val completo = titulo.isNotBlank() && personagens.any { it.id == personagemId } && livros.any { it.id == livroId }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CabecalhoTela("Nova campanha", "Um personagem. Um mundo. Uma história sua.", aoVoltar)
        OutlinedTextField(value = titulo, onValueChange = { titulo = it }, label = { Text("Título da campanha *") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("1. Seu personagem *", style = MaterialTheme.typography.titleMedium)
        if (personagens.isEmpty()) EstadoVazio("Crie sua primeira ficha", "Uma campanha precisa de um personagem.",
            "Criar personagem", aoCriarPersonagem)
        personagens.forEach { personagem ->
            OutlinedCard(onClick = { personagemId = personagem.id }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp)) {
                    RadioButton(selected = personagemId == personagem.id, onClick = { personagemId = personagem.id })
                    Column(Modifier.padding(start = 8.dp, top = 4.dp)) {
                        Text(personagem.nome, style = MaterialTheme.typography.titleMedium)
                        Text(personagem.classe.ifBlank { "Classe livre" })
                    }
                }
            }
        }
        Text("2. Sua aventura *", style = MaterialTheme.typography.titleMedium)
        livros.forEach { livro ->
            OutlinedCard(onClick = { livroId = livro.id }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp)) {
                    RadioButton(selected = livroId == livro.id, onClick = { livroId = livro.id })
                    Column(Modifier.padding(start = 8.dp, top = 4.dp)) {
                        Text(livro.titulo, style = MaterialTheme.typography.titleMedium)
                        Text(livro.genero)
                    }
                }
            }
        }
        if (!completo) Text("Preencha o título e selecione um personagem e uma aventura.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        BotaoSalvar("Iniciar campanha", habilitado = completo, salvando = salvando) {
            aoIniciar(titulo.trim(), personagemId, livroId)
        }
    }
}
