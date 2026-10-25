package com.example.monoco.ui.telas.perfil

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.monoco.dados.local.PerfilLocal
import com.example.monoco.dominio.RegrasMonoco
import com.example.monoco.ui.componentes.*

@Composable
fun TelaPerfil(perfil: PerfilLocal, salvando: Boolean, aoSalvar: (String) -> Unit,
    aoConfiguracoes: () -> Unit, aoSair: () -> Unit) {
    var nome by rememberSaveable(perfil.nome) { mutableStateOf(perfil.nome) }
    var tentouSalvar by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        CabecalhoTela("Seu perfil", "Como você quer ser chamado nas suas aventuras?")
        AvisoLocal("Este é um perfil local. Conta, e-mail e alteração de senha estarão disponíveis com as contas online.")
        OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome") },
            singleLine = true, isError = tentouSalvar && RegrasMonoco.erroNome(nome) != null,
            supportingText = { if (tentouSalvar) RegrasMonoco.erroNome(nome)?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth())
        BotaoSalvar("Salvar nome", salvando = salvando) {
            tentouSalvar = true
            if (RegrasMonoco.erroNome(nome) == null) aoSalvar(nome)
        }
        OutlinedCard(onClick = aoConfiguracoes, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Configurações do narrador", style = MaterialTheme.typography.titleMedium)
                Text("Tom, tamanho das respostas e configuração de provedor.")
                Text("Abrir configurações →", color = MaterialTheme.colorScheme.primary)
            }
        }
        TextButton(onClick = aoSair) { Text("Voltar às boas-vindas") }
        Text("Seu progresso local continuará salvo.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
