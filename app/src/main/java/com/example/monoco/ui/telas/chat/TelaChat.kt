package com.example.monoco.ui.telas.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.monoco.dados.local.*
import com.example.monoco.dominio.RegrasMonoco
import com.example.monoco.ui.componentes.*

@Composable
fun TelaChat(campanha: Campanha, personagem: Personagem, livro: Livro,
    mensagens: List<Mensagem>, totalMensagens: Int, quantidadeMemorias: Int,
    narrando: Boolean, narracaoEmOutraCampanha: Boolean, trechoNarracao: String,
    aoVoltar: () -> Unit, aoMemorias: () -> Unit, aoConfiguracoes: () -> Unit,
    aoSalvarComoMemoria: (Long) -> Unit, aoCarregarAnteriores: () -> Unit,
    aoEnviar: (String, () -> Unit) -> Unit, aoParar: () -> Unit, aoTentarNovamente: () -> Unit,
    rotuloNarrador: String = "Demonstração · sem IA", rotuloParar: String = "Parar narração") {
    var acao by rememberSaveable(campanha.id) { mutableStateOf("") }
    var verFicha by rememberSaveable { mutableStateOf(false) }
    val lista = rememberLazyListState()
    val respostaPendente = mensagens.lastOrNull()?.autor == Mensagem.JOGADOR && !narrando
    val envioHabilitado = RegrasMonoco.erroAcao(acao) == null && !narrando &&
        !narracaoEmOutraCampanha && !respostaPendente
    fun enviar() { if (envioHabilitado) aoEnviar(acao) { acao = "" } }

    LaunchedEffect(mensagens.lastOrNull()?.id, trechoNarracao) {
        val quantidade = lista.layoutInfo.totalItemsCount
        if (quantidade > 0) lista.animateScrollToItem(quantidade - 1)
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CabecalhoTela(campanha.titulo, "${personagem.nome} · ${livro.titulo}", aoVoltar)
            Etiqueta(rotuloNarrador)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { verFicha = true }) { Text("Ver ficha") }
                TextButton(onClick = aoMemorias) { Text("Memórias ($quantidadeMemorias)") }
                TextButton(onClick = aoConfiguracoes) { Text("IA") }
            }
        }
        LazyColumn(state = lista, modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (mensagens.size < totalMensagens) item {
                TextButton(onClick = aoCarregarAnteriores, modifier = Modifier.fillMaxWidth()) {
                    Text("Carregar mensagens anteriores")
                }
            }
            items(mensagens, key = { it.id }) { mensagem ->
                BalaoMensagem(mensagem.texto, mensagem.autor == Mensagem.JOGADOR,
                    mensagem.interrompida, aoSalvarMemoria =
                        if (mensagem.autor == Mensagem.MESTRE) ({ aoSalvarComoMemoria(mensagem.id) }) else null)
            }
            if (narrando) item {
                BalaoMensagem(trechoNarracao.ifBlank { "O narrador está preparando a cena…" }, false)
            }
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (respostaPendente) OutlinedButton(onClick = aoTentarNovamente, enabled = !narracaoEmOutraCampanha,
                modifier = Modifier.fillMaxWidth()) { Text("Gerar resposta da ação salva / tentar novamente") }
            if (narracaoEmOutraCampanha) Text("Há uma narração em andamento em outra campanha.",
                style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(value = acao, onValueChange = { acao = it },
                label = { Text("O que seu personagem faz?") },
                placeholder = { Text("Descreva sua ação ou fala…") },
                modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 5,
                isError = acao.length > RegrasMonoco.LIMITE_ACAO,
                supportingText = { Text("${acao.length} / ${RegrasMonoco.LIMITE_ACAO}") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { enviar() }))
            if (narrando) OutlinedButton(onClick = aoParar, modifier = Modifier.fillMaxWidth()) { Text(rotuloParar) }
            else Button(onClick = { enviar() }, enabled = envioHabilitado, modifier = Modifier.fillMaxWidth()) { Text("Enviar ação") }
        }
    }
    if (verFicha) AlertDialog(onDismissRequest = { verFicha = false },
        title = { Text(personagem.nome) },
        text = {
            SelectionContainer {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Classe: ${personagem.classe.ifBlank { "Não informada" }}")
                    Text("Atributos: ${personagem.atributos.ifBlank { "Não informados" }}")
                    Text("História: ${personagem.historia.ifBlank { "Não informada" }}")
                    Text("Segredos: ${personagem.segredos.ifBlank { "Não informados" }}")
                }
            }
        }, confirmButton = { TextButton(onClick = { verFicha = false }) { Text("Fechar ficha") } })
}

@Composable
private fun BalaoMensagem(texto: String, doJogador: Boolean, interrompida: Boolean = false,
    aoSalvarMemoria: (() -> Unit)? = null) {
    Box(Modifier.fillMaxWidth(), contentAlignment = if (doJogador) Alignment.CenterEnd else Alignment.CenterStart) {
        Surface(modifier = Modifier.fillMaxWidth(if (doJogador) 0.9f else 1f),
            shape = MaterialTheme.shapes.large,
            color = if (doJogador) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (doJogador) "VOCÊ" else "MESTRE", style = MaterialTheme.typography.labelSmall,
                    color = if (doJogador) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)
                SelectionContainer { Text(texto, style = MaterialTheme.typography.bodyLarge) }
                if (interrompida) Text("Narração interrompida · trecho salvo", style = MaterialTheme.typography.labelSmall)
                if (aoSalvarMemoria != null) TextButton(onClick = aoSalvarMemoria) { Text("Salvar como memória") }
            }
        }
    }
}
