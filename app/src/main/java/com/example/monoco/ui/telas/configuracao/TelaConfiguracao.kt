package com.example.monoco.ui.telas.configuracao

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.monoco.dados.local.Preferencias
import com.example.monoco.dados.remoto.ConfiguracaoOnline
import com.example.monoco.ui.componentes.*
import java.util.Locale

@Composable
fun TelaConfiguracao(preferencias: Preferencias,salvando: Boolean,aoVoltar: ()->Unit,
    aoSalvar: (Preferencias)->Unit,aoTestarChave: ()->Unit,
    configuracaoOnline: ConfiguracaoOnline?=null,
    aoConfigurarProvedor: (String,String,String)->Unit={ _,_,_-> },aoRemoverChave: ()->Unit={}) {
    var tom by rememberSaveable { mutableStateOf(preferencias.tom) }
    var tamanho by rememberSaveable { mutableStateOf(preferencias.tamanhoResposta) }
    var criatividade by rememberSaveable { mutableStateOf(preferencias.criatividade) }
    var provedor by rememberSaveable(configuracaoOnline?.provedor) { mutableStateOf(configuracaoOnline?.provedor ?: "OpenAI") }
    var modelo by rememberSaveable(configuracaoOnline?.modelo) { mutableStateOf(configuracaoOnline?.modelo ?: "") }
    var chave by remember { mutableStateOf("") }
    LaunchedEffect(configuracaoOnline?.finalChave) { chave="" }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
        verticalArrangement=Arrangement.spacedBy(16.dp)) {
        CabecalhoTela("Seu mestre, seu estilo","Ajuste como prefere viver suas histórias.",aoVoltar)
        Etiqueta(if(configuracaoOnline?.provedor=="Demonstracao") "Demonstração · sem IA" else "IA: ${configuracaoOnline?.provedor ?: "não configurada"}")
        Text("Tom da narração",style=MaterialTheme.typography.titleMedium)
        EscolhaEmChips(listOf("Aventura","Sombrio","Leve"),tom) { tom=it }
        Text("Tamanho das respostas",style=MaterialTheme.typography.titleMedium)
        EscolhaEmChips(listOf("Curta","Média","Longa"),tamanho) { tamanho=it }
        Text("Criatividade: ${String.format(Locale.forLanguageTag("pt-BR"),"%.1f",criatividade)}")
        Slider(value=criatividade,onValueChange={ criatividade=it },valueRange=0f..1f,steps=9)
        TextButton(onClick={ tom="Aventura"; tamanho="Média"; criatividade=0.7f }) { Text("Restaurar estilo padrão") }
        BotaoSalvar("Salvar preferências",salvando=salvando) {
            aoSalvar(Preferencias(tom=tom,tamanhoResposta=tamanho,criatividade=criatividade,
                provedorPlanejado=configuracaoOnline?.provedor ?: provedor,modeloPlanejado=configuracaoOnline?.modelo ?: modelo))
        }
        HorizontalDivider()
        Text("Conexão com a IA",style=MaterialTheme.typography.titleLarge)
        val provedores=listOf("OpenAI","OpenRouter")+if(configuracaoOnline?.demonstracaoPermitida==true) listOf("Demonstracao") else emptyList()
        EscolhaEmChips(provedores,provedor) { provedor=it }
        if(provedor=="Demonstracao") Text("Respostas roteirizadas para testar o aplicativo, sem chamadas a um modelo de IA.")
        else {
            OutlinedTextField(modelo,{ modelo=it },label={ Text("Identificador do modelo") },singleLine=true,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(chave,{ chave=it },label={ Text("Nova chave de API") },singleLine=true,
                visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
            if(configuracaoOnline?.chaveConfigurada==true) Text("Chave salva: ••••${configuracaoOnline.finalChave}")
            Text("Testar consulta os modelos disponíveis. Gerar narrações usa os créditos da sua conta no provedor.",style=MaterialTheme.typography.bodySmall)
        }
        Button(onClick={ aoConfigurarProvedor(provedor,modelo.trim(),chave.trim()) },
            enabled=!salvando && (provedor=="Demonstracao" || (modelo.isNotBlank() && chave.isNotBlank())),modifier=Modifier.fillMaxWidth()) {
            Text(if(salvando) "Validando…" else "Testar e salvar conexão")
        }
        if(configuracaoOnline?.chaveConfigurada==true) TextButton(onClick=aoRemoverChave,enabled=!salvando) { Text("Remover chave salva") }
    }
}
