package com.example.monoco.ui.telas.login

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
import androidx.compose.foundation.text.KeyboardActions
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

@Composable
fun TelaLogin(
    aoVoltar: () -> Unit,
    aoCadastrar: () -> Unit,
    aoEntrar: (email: String, senha: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var email by rememberSaveable { mutableStateOf("") }
    // A senha fica somente na memória da tela, sem entrar no estado salvo.
    var senha by remember { mutableStateOf("") }

    var erroEmail by rememberSaveable { mutableStateOf<String?>(null) }
    var erroSenha by rememberSaveable { mutableStateOf<String?>(null) }

    fun formularioEstaValido(): Boolean {
        erroEmail = when {
            email.isBlank() -> "Informe seu e-mail."
            !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Informe um e-mail válido."
            else -> null
        }

        erroSenha = if (senha.isBlank()) {
            "Informe sua senha."
        } else {
            null
        }

        return erroEmail == null && erroSenha == null
    }

    fun tentarEntrar() {
        if (formularioEstaValido()) {
            aoEntrar(email.trim(), senha)
        }
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
            text = "Entre na sua conta",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Use seu e-mail e sua senha para continuar suas aventuras.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                erroEmail = null
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
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { tentarEntrar() }),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { tentarEntrar() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Entrar")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = aoCadastrar,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Ainda não tenho conta")
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviaTelaLogin() {
    MonocoTheme {
        TelaLogin(
            aoVoltar = {},
            aoCadastrar = {},
            aoEntrar = { _, _ -> }
        )
    }
}
