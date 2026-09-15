package com.example.monoco

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.monoco.ui.telas.boasvindas.TelaBoasVindas
import com.example.monoco.ui.telas.cadastro.TelaCadastro

private object Rotas {
    const val BOAS_VINDAS = "boas_vindas"
    const val CADASTRO = "cadastro"
}

@Composable
fun MonocoApp(modifier: Modifier = Modifier) {
    val controladorNavegacao = rememberNavController()

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
        }
    }
}
