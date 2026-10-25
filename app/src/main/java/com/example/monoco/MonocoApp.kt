package com.example.monoco

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.monoco.ui.telas.boasvindas.TelaBoasVindas
import com.example.monoco.ui.telas.cadastro.TelaCadastro
import com.example.monoco.ui.telas.login.TelaLogin

private object Rotas {
    const val BOAS_VINDAS = "boas_vindas"
    const val CADASTRO = "cadastro"
    const val LOGIN = "login"
}

@Composable
fun MonocoApp(modifier: Modifier = Modifier) {
    val controladorNavegacao = rememberNavController()
    val contexto = LocalContext.current

    Scaffold(modifier = modifier.fillMaxSize()) { espacamentoInterno ->
        NavHost(
            navController = controladorNavegacao,
            startDestination = Rotas.BOAS_VINDAS,
            modifier = Modifier.padding(espacamentoInterno)
        ) {
            composable(route = Rotas.BOAS_VINDAS) {
                TelaBoasVindas(
                    aoComecarAventura = {
                        controladorNavegacao.navigate(Rotas.CADASTRO)
                    },
                    aoEntrar = {
                        controladorNavegacao.navigate(Rotas.LOGIN)
                    }
                )
            }

            composable(route = Rotas.CADASTRO) {
                TelaCadastro(
                    aoVoltar = {
                        controladorNavegacao.popBackStack()
                    },
                    aoCadastrar = { _, _, _ ->
                    }
                )
            }

            composable(route = Rotas.LOGIN) {
                TelaLogin(
                    aoVoltar = {
                        controladorNavegacao.popBackStack()
                    },
                    aoCadastrar = {
                        controladorNavegacao.navigate(Rotas.CADASTRO)
                    },
                    aoEntrar = { _, _ ->
                        Toast.makeText(
                            contexto,
                            "A autenticação ainda não está conectada ao servidor.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }
    }
}
