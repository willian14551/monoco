package com.example.monoco.ui.telas.memorias

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.monoco.dados.local.Memoria
import com.example.monoco.ui.componentes.*

@Composable
fun TelaMemorias(tituloCampanha: String, memorias: List<Memoria>, aoVoltar: () -> Unit,
    aoCriar: () -> Unit, aoEditar: (Long) -> Unit, aoAlternar: (Memoria) -> Unit, aoExcluir: (Long) -> Unit) {
    var excluirId by rememberSaveable { mutableStateOf<Long?>(null) }
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { CabecalhoTela("Memórias", tituloCampanha, aoVoltar) }
        item { AvisoLocal("Somente as memórias ativas entram no contexto da próxima narração.") }
        item { Button(onClick = aoCriar, modifier = Modifier.fillMaxWidth()) { Text("+ Nova memória") } }
        if (memorias.isEmpty()) item {
            EstadoVazio("Guarde os fatos importantes", "Crie uma memória ou salve uma narração diretamente pelo chat.")
        }
        items(memorias, key = { it.id }) { memoria ->
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(memoria.titulo, style = MaterialTheme.typography.titleMedium)
                    Text(memoria.texto)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (memoria.ativa) "Ativa para o mestre" else "Inativa")
                        Switch(checked = memoria.ativa, onCheckedChange = { aoAlternar(memoria) },
                            modifier = Modifier.semantics { contentDescription = "Ativar memória ${memoria.titulo}" })
                    }
                    Row {
                        TextButton(onClick = { aoEditar(memoria.id) }) { Text("Editar") }
                        TextButton(onClick = { excluirId = memoria.id }) { Text("Excluir") }
                    }
                }
            }
        }
    }
    excluirId?.let { id -> ConfirmarExclusao("Excluir memória?", "O mestre deixará de receber este fato no contexto.",
        aoCancelar = { excluirId = null }, aoConfirmar = { aoExcluir(id); excluirId = null }) }
}
