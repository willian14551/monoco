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

        fun obter(contexto: Context): BancoMonoco = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(
                contexto.applicationContext, BancoMonoco::class.java, "monoco.db"
            ).build().also { instancia = it }
        }
    }
}
