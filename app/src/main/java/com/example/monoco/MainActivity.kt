package com.example.monoco

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.monoco.dados.remoto.ApiMonoco
import com.example.monoco.dados.remoto.SessaoSegura
import com.example.monoco.ui.theme.MonocoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var cofre: SessaoSegura
    @Inject lateinit var api: ApiMonoco

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            MonocoTheme {
                MonocoApp(cofre, api)
            }
        }
    }
}
