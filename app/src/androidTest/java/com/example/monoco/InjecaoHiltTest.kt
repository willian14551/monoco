package com.example.monoco

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.monoco.dados.remoto.ApiMonoco
import com.example.monoco.dados.remoto.SessaoSegura
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InjecaoHiltTest {
    @Test
    fun dependenciasInjetadasSaoMantidasAoRecriarAtividade() {
        ActivityScenario.launch(MainActivity::class.java).use { cenario ->
            lateinit var cofre: SessaoSegura
            lateinit var api: ApiMonoco

            cenario.onActivity { atividade ->
                assertTrue(atividade.application is AplicacaoMonoco)
                cofre = atividade.cofre
                api = atividade.api
            }

            cenario.recreate()

            cenario.onActivity { atividade ->
                assertSame(cofre, atividade.cofre)
                assertSame(api, atividade.api)
            }
        }
    }
}
