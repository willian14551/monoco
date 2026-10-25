package com.example.monoco.ui.telas.personagens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.monoco.dados.local.Campanha
import com.example.monoco.dados.local.Personagem
import com.example.monoco.ui.componentes.*

@Composable
fun TelaPersonagens(personagens: List<Personagem>, campanhas: List<Campanha>,
    aoCriar: () -> Unit, aoEditar: (Long) -> Unit, aoExcluir: (Long) -> Unit) {
    var excluirId by rememberSaveable { mutableStateOf<Long?>(null) }
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { CabecalhoTela("Personagens", "Dê um rosto, uma história e um propósito à sua aventura.") }
        item { Button(onClick = aoCriar, modifier = Modifier.fillMaxWidth()) { Text("+ Novo personagem") } }
        if (personagens.isEmpty()) item {
            EstadoVazio("Quem você será?", "Seu personagem pode participar de mais de uma campanha.", "Criar personagem", aoCriar)
        }
        items(personagens, key = { it.id }) { personagem ->
            OutlinedCard(onClick = { aoEditar(personagem.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(personagem.nome, style = MaterialTheme.typography.titleLarge)
                    Text(personagem.classe.ifBlank { "Classe livre" }, color = MaterialTheme.colorScheme.secondary)
                    val quantidade = campanhas.count { it.personagemId == personagem.id }
                    Text("$quantidade campanha(s) vinculada(s)", style = MaterialTheme.typography.bodySmall)
                    if (personagem.historia.isNotBlank()) Text(personagem.historia, maxLines = 3)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = { aoEditar(personagem.id) }) { Text("Editar ficha") }
                        TextButton(onClick = { excluirId = personagem.id }) { Text("Excluir") }
                    }
                }
            }
        }
    }
    excluirId?.let { id ->
        val vinculadas = campanhas.count { it.personagemId == id }
        if (vinculadas > 0) AlertDialog(onDismissRequest = { excluirId = null },
            title = { Text("Personagem em campanha") },
            text = { Text("Há $vinculadas campanha(s) usando esta ficha. Exclua essas campanhas antes de remover o personagem.") },
            confirmButton = { TextButton(onClick = { excluirId = null }) { Text("Entendi") } })
        else ConfirmarExclusao("Excluir personagem?", "Esta ficha será apagada deste aparelho.",
            aoCancelar = { excluirId = null }, aoConfirmar = { aoExcluir(id); excluirId = null })
    }
}
