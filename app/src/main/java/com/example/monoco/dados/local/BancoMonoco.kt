package com.example.monoco.dados.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Personagem::class, Livro::class, Campanha::class, Mensagem::class,
        Memoria::class, PerfilLocal::class, Preferencias::class],
    version = 1,
    exportSchema = true
)
abstract class BancoMonoco : RoomDatabase() {
    abstract fun monocoDao(): MonocoDao

    companion object {
        @Volatile private var instancia: BancoMonoco? = null

        fun paraConta(contexto: Context, usuarioId: Long, servidor: String): BancoMonoco {
            val origem=java.security.MessageDigest.getInstance("SHA-256").digest(servidor.toByteArray())
                .take(16).joinToString("") { "%02x".format(it) }
            return Room.databaseBuilder(contexto.applicationContext,BancoMonoco::class.java,"monoco-conta-$origem-$usuarioId.db").build()
        }

        fun obter(contexto: Context): BancoMonoco = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(
                contexto.applicationContext, BancoMonoco::class.java, "monoco.db"
            ).build().also { instancia = it }
        }
    }
}
