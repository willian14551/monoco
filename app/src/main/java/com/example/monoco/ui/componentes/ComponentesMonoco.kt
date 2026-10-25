package com.example.monoco.ui.componentes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CabecalhoTela(titulo: String, subtitulo: String? = null, aoVoltar: (() -> Unit)? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (aoVoltar != null) TextButton(onClick = aoVoltar, contentPadding = PaddingValues(0.dp)) {
            Text("‹ Voltar")
        }
        Text(titulo, style = MaterialTheme.typography.headlineMedium)
        if (subtitulo != null) Text(subtitulo, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AvisoLocal(texto: String = "Modo local · Seus dados ficam neste aparelho.") {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Text(texto, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
fun EstadoVazio(titulo: String, descricao: String, textoBotao: String? = null, aoClicar: () -> Unit = {}) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleLarge)
            Text(descricao, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (textoBotao != null) Button(onClick = aoClicar) { Text(textoBotao) }
        }
    }
}

@Composable
fun ConfirmarExclusao(titulo: String, descricao: String, aoCancelar: () -> Unit, aoConfirmar: () -> Unit) {
    AlertDialog(onDismissRequest = aoCancelar,
        title = { Text(titulo) }, text = { Text(descricao) },
        confirmButton = { TextButton(onClick = aoConfirmar) {
            Text("Excluir", color = MaterialTheme.colorScheme.error)
        } }, dismissButton = { TextButton(onClick = aoCancelar) { Text("Cancelar") } })
}

@Composable
fun Etiqueta(texto: String) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
        Text(texto, Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EscolhaEmChips(opcoes: List<String>, selecionado: String, aoSelecionar: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        opcoes.forEach { opcao ->
            FilterChip(selected = opcao == selecionado, onClick = { aoSelecionar(opcao) },
                label = { Text(opcao) })
        }
    }
}

@Composable
fun BotaoSalvar(texto: String = "Salvar", habilitado: Boolean = true,
    salvando: Boolean = false, aoClicar: () -> Unit) {
    Button(onClick = aoClicar, enabled = habilitado && !salvando,
        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) {
        if (salvando) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
        }
        Text(if (salvando) "Salvando…" else texto)
    }
}
