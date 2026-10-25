package com.example.monoco.ui.telas.memorias

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.monoco.dados.local.Memoria
import com.example.monoco.dominio.RegrasMonoco
import com.example.monoco.ui.componentes.*

@Composable
fun TelaEditarMemoria(campanhaId: Long, memoria: Memoria?, textoInicial: String, salvando: Boolean,
    aoVoltar: () -> Unit, aoSalvar: (Memoria) -> Unit) {
    var titulo by rememberSaveable { mutableStateOf(memoria?.titulo.orEmpty()) }
    var texto by rememberSaveable { mutableStateOf(memoria?.texto ?: textoInicial) }
    var ativa by rememberSaveable { mutableStateOf(memoria?.ativa ?: true) }
    var tentouSalvar by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CabecalhoTela(if (memoria == null) "Nova memória" else "Editar memória",
            "Registre fatos, relações e objetivos que devem acompanhar a aventura.", aoVoltar)
        OutlinedTextField(value = titulo, onValueChange = { titulo = it }, label = { Text("Título *") },
            singleLine = true, isError = tentouSalvar && titulo.isBlank(),
            supportingText = { if (tentouSalvar && titulo.isBlank()) Text("Informe o título.") },
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = texto, onValueChange = { texto = it }, label = { Text("O que lembrar? *") },
            minLines = 6, isError = tentouSalvar && texto.isBlank(),
            supportingText = { if (tentouSalvar && texto.isBlank()) Text("Descreva a memória.") },
            modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Disponível para o mestre")
            Switch(checked = ativa, onCheckedChange = { ativa = it },
                modifier = Modifier.semantics { contentDescription = "Disponibilizar memória para o mestre" })
        }
        BotaoSalvar("Salvar memória", salvando = salvando) {
            tentouSalvar = true
            if (RegrasMonoco.erroMemoria(titulo, texto) == null) aoSalvar(
                Memoria(memoria?.id ?: 0, campanhaId, titulo, texto, ativa))
        }
    }
}
