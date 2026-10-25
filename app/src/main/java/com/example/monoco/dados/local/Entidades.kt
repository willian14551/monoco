package com.example.monoco.dados.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "personagens")
data class Personagem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val classe: String = "",
    val atributos: String = "",
    val historia: String = "",
    val segredos: String = "",
    val compartilharNome: Boolean = true,
    val compartilharClasse: Boolean = true,
    val compartilharAtributos: Boolean = true,
    val compartilharHistoria: Boolean = true,
    val compartilharSegredos: Boolean = true
)

@Entity(tableName = "livros")
data class Livro(
    @PrimaryKey val id: Long,
    val titulo: String,
    val genero: String,
    val descricao: String,
    val premissa: String,
    val regras: String,
    val abertura: String,
    val duracao: String
)

@Entity(
    tableName = "campanhas",
    foreignKeys = [
        ForeignKey(entity = Personagem::class, parentColumns = ["id"],
            childColumns = ["personagemId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = Livro::class, parentColumns = ["id"],
            childColumns = ["livroId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("personagemId"), Index("livroId")]
)
data class Campanha(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val personagemId: Long,
    val livroId: Long,
    val ultimaAtividade: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "mensagens",
    foreignKeys = [ForeignKey(entity = Campanha::class, parentColumns = ["id"],
        childColumns = ["campanhaId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["campanhaId", "id"])]
)
data class Mensagem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val campanhaId: Long,
    val autor: String,
    val texto: String,
    val criadaEm: Long = System.currentTimeMillis(),
    val interrompida: Boolean = false
) {
    companion object {
        const val JOGADOR = "jogador"
        const val MESTRE = "mestre"
    }
}

@Entity(
    tableName = "memorias",
    foreignKeys = [ForeignKey(entity = Campanha::class, parentColumns = ["id"],
        childColumns = ["campanhaId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("campanhaId")]
)
data class Memoria(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val campanhaId: Long,
    val titulo: String,
    val texto: String,
    val ativa: Boolean = true
)

// Este perfil pertence apenas à instalação local. Não representa uma conta autenticada.
@Entity(tableName = "perfil_local")
data class PerfilLocal(@PrimaryKey val id: Int = 1, val nome: String = "Aventureiro")

@Entity(tableName = "preferencias")
data class Preferencias(
    @PrimaryKey val id: Int = 1,
    val tom: String = "Aventura",
    val tamanhoResposta: String = "Média",
    val criatividade: Float = 0.7f,
    val provedorPlanejado: String = "OpenAI",
    val modeloPlanejado: String = ""
)
