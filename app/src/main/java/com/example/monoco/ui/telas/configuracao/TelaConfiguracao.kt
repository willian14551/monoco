package com.example.monoco.ui.telas.configuracao

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.monoco.dados.local.Preferencias
import com.example.monoco.ui.componentes.*
import java.util.Locale

@Composable
fun TelaConfiguracao(preferencias: Preferencias, salvando: Boolean, aoVoltar: () -> Unit,
    aoSalvar: (Preferencias) -> Unit, aoTestarChave: () -> Unit) {
    var tom by rememberSaveable { mutableStateOf(preferencias.tom) }
    var tamanho by rememberSaveable { mutableStateOf(preferencias.tamanhoResposta) }
    var criatividade by rememberSaveable { mutableStateOf(preferencias.criatividade) }
    var provedor by rememberSaveable { mutableStateOf(preferencias.provedorPlanejado) }
    var modelo by rememberSaveable { mutableStateOf(preferencias.modeloPlanejado) }
    // A chave é transitória. Não a gravamos nem afirmamos ter validado um provedor.
    var chave by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CabecalhoTela("Seu mestre, seu estilo", "Ajuste como prefere viver suas histórias.", aoVoltar)
        Etiqueta("Narrador de demonstração ativo")
        Text("Tom da narração", style = MaterialTheme.typography.titleMedium)
        EscolhaEmChips(listOf("Aventura", "Sombrio", "Leve"), tom) { tom = it }
        Text("Tamanho das respostas", style = MaterialTheme.typography.titleMedium)
        EscolhaEmChips(listOf("Curta", "Média", "Longa"), tamanho) { tamanho = it }
        Text("Criatividade: ${String.format(Locale.forLanguageTag("pt-BR"), "%.1f", criatividade)}",
            style = MaterialTheme.typography.titleMedium)
        Slider(value = criatividade, onValueChange = { criatividade = it }, valueRange = 0f..1f, steps = 9)
        Text("A criatividade será usada quando a IA estiver conectada. O roteiro local demonstra tom e tamanho.",
            style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = {
            val padrao = Preferencias()
            tom = padrao.tom; tamanho = padrao.tamanhoResposta; criatividade = padrao.criatividade
        }) { Text("Restaurar estilo padrão") }
        Text("Provedor de IA", style = MaterialTheme.typography.titleLarge)
        AvisoLocal("A conexão com provedores estará disponível em uma próxima versão. Nenhuma chave é salva ou enviada por esta tela.")
        EscolhaEmChips(listOf("OpenAI", "OpenRouter"), provedor) { provedor = it }
        OutlinedTextField(value = modelo, onValueChange = { modelo = it }, label = { Text("Modelo planejado") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = chave, onValueChange = { chave = it }, label = { Text("Chave de API (não será salva)") },
            singleLine = true, visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = { aoTestarChave(); chave = "" }, enabled = chave.isNotBlank(),
            modifier = Modifier.fillMaxWidth()) { Text("Ver disponibilidade da conexão") }
        BotaoSalvar("Salvar preferências locais", salvando = salvando) {
            aoSalvar(Preferencias(tom = tom, tamanhoResposta = tamanho, criatividade = criatividade,
                provedorPlanejado = provedor, modeloPlanejado = modelo.trim()))
        }
    }
}
