package com.example.monoco.ui.telas.perfil

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.monoco.ui.componentes.*

@Composable
fun TelaPerfilOnline(nomeAtual: String,emailAtual: String,salvando: Boolean,
    aoSalvar: (String,String)->Unit,aoAlterarSenha: (String,String)->Unit,
    aoConfiguracoes: ()->Unit,aoSair: ()->Unit) {
    var nome by rememberSaveable(nomeAtual) { mutableStateOf(nomeAtual) }
    var email by rememberSaveable(emailAtual) { mutableStateOf(emailAtual) }
    var senhaAtual by remember { mutableStateOf("") }
    var novaSenha by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
        verticalArrangement=Arrangement.spacedBy(16.dp)) {
        CabecalhoTela("Seu perfil","Sua conta e suas preferências.")
        OutlinedTextField(nome,{ nome=it },label={ Text("Nome") },singleLine=true,modifier=Modifier.fillMaxWidth())
        OutlinedTextField(email,{ email=it },label={ Text("E-mail") },singleLine=true,modifier=Modifier.fillMaxWidth())
        BotaoSalvar("Salvar perfil",salvando=salvando) { aoSalvar(nome.trim(),email.trim()) }
        HorizontalDivider()
        Text("Alterar senha",style=MaterialTheme.typography.titleMedium)
        OutlinedTextField(senhaAtual,{ senhaAtual=it },label={ Text("Senha atual") },singleLine=true,
            visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
        OutlinedTextField(novaSenha,{ novaSenha=it },label={ Text("Nova senha (mínimo 8 caracteres)") },singleLine=true,
            visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
        OutlinedButton(onClick={ aoAlterarSenha(senhaAtual,novaSenha) },enabled=!salvando && senhaAtual.isNotEmpty() && novaSenha.length>=8,
            modifier=Modifier.fillMaxWidth()) { Text("Alterar senha e entrar novamente") }
        OutlinedButton(onClick=aoConfiguracoes,modifier=Modifier.fillMaxWidth()) { Text("Configurações da IA") }
        TextButton(onClick=aoSair,enabled=!salvando) { Text("Sair da conta") }
    }
}
