package com.example.monoco.ui.telas.cadastro

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.monoco.ui.theme.MonocoTheme
import com.example.monoco.ui.componentes.AvisoLocal

private const val TAMANHO_MINIMO_SENHA = 8

@Composable
fun TelaCadastro(
    aoVoltar: () -> Unit,
    aoCadastrar: (nome: String, email: String, senha: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var nome by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var confirmacaoSenha by remember { mutableStateOf("") }

    var erroNome by rememberSaveable { mutableStateOf<String?>(null) }
    var erroEmail by rememberSaveable { mutableStateOf<String?>(null) }
    var erroSenha by rememberSaveable { mutableStateOf<String?>(null) }
    var erroConfirmacao by rememberSaveable { mutableStateOf<String?>(null) }
    var formularioValidado by rememberSaveable { mutableStateOf(false) }

    fun formularioEstaValido(): Boolean {
        erroNome = if (nome.trim().length < 2) {
            "Informe um nome com pelo menos 2 caracteres."
        } else {
            null
        }

        erroEmail = when {
            email.isBlank() -> "Informe seu e-mail."
            !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Informe um e-mail válido."
            else -> null
        }

        erroSenha = when {
            senha.isBlank() -> "Informe uma senha."
            senha.length < TAMANHO_MINIMO_SENHA -> "Use pelo menos 8 caracteres."
            else -> null
        }

        erroConfirmacao = when {
            confirmacaoSenha.isBlank() -> "Confirme sua senha."
            confirmacaoSenha != senha -> "As senhas não coincidem."
            else -> null
        }

        return erroNome == null &&
            erroEmail == null &&
            erroSenha == null &&
            erroConfirmacao == null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        TextButton(onClick = aoVoltar) {
            Text(text = "Voltar")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Crie sua conta",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Salve seus personagens e continue suas aventuras depois.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        AvisoLocal("As contas online ainda não estão disponíveis.")

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = nome,
            onValueChange = {
                nome = it
                erroNome = null
                formularioValidado = false
            },
            label = { Text("Nome") },
            singleLine = true,
            isError = erroNome != null,
            supportingText = erroNome?.let { mensagem ->
                { Text(mensagem) }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                erroEmail = null
                formularioValidado = false
            },
            label = { Text("E-mail") },
            singleLine = true,
            isError = erroEmail != null,
            supportingText = erroEmail?.let { mensagem ->
                { Text(mensagem) }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = senha,
            onValueChange = {
                senha = it
                erroSenha = null
                formularioValidado = false
            },
            label = { Text("Senha") },
            singleLine = true,
            isError = erroSenha != null,
            supportingText = erroSenha?.let { mensagem ->
                { Text(mensagem) }
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmacaoSenha,
            onValueChange = {
                confirmacaoSenha = it
                erroConfirmacao = null
                formularioValidado = false
            },
            label = { Text("Confirmar senha") },
            singleLine = true,
            isError = erroConfirmacao != null,
            supportingText = erroConfirmacao?.let { mensagem ->
                { Text(mensagem) }
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                formularioValidado = formularioEstaValido()

                if (formularioValidado) {
                    aoCadastrar(nome.trim(), email.trim(), senha)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Criar conta")
        }

        if (formularioValidado) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Campos válidos. Ainda não disponivel.",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviaTelaCadastro() {
    MonocoTheme {
        TelaCadastro(
            aoVoltar = {},
            aoCadastrar = { _, _, _ -> }
        )
    }
}
