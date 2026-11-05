package com.example.monoco.injecao

import android.content.Context
import com.example.monoco.dados.local.BancoMonoco
import com.example.monoco.dados.remoto.SessaoSegura
import com.example.monoco.dominio.Narrador
import com.example.monoco.dominio.NarradorDemonstracao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.ViewModelLifecycle
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object ModuloDados {
    @Provides
    @ViewModelScoped
    fun fornecerBanco(
        @ApplicationContext contexto: Context,
        cofre: SessaoSegura,
        ciclo: ViewModelLifecycle
    ): BancoMonoco {
        val conta = requireNotNull(cofre.sessao.value) { "Entre na sua conta para acessar as aventuras." }
        val banco = BancoMonoco.paraConta(contexto, conta.usuario.id, cofre.endereco)
        ciclo.addOnClearedListener { banco.close() }
        return banco
    }

    @Provides
    fun fornecerNarrador(): Narrador = NarradorDemonstracao()
}
