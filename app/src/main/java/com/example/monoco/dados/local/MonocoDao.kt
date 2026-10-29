package com.example.monoco.dados.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MonocoDao {
    @Query("SELECT * FROM personagens ORDER BY nome COLLATE NOCASE, id")
    fun observarPersonagens(): Flow<List<Personagem>>

    @Query("SELECT * FROM livros ORDER BY id")
    fun observarLivros(): Flow<List<Livro>>

    @Query("SELECT * FROM campanhas ORDER BY ultimaAtividade DESC, id DESC")
    fun observarCampanhas(): Flow<List<Campanha>>

    @Query("SELECT * FROM (SELECT * FROM mensagens WHERE campanhaId = :campanhaId ORDER BY id DESC LIMIT :limite) ORDER BY id")
    fun observarMensagens(campanhaId: Long, limite: Int): Flow<List<Mensagem>>

    @Query("SELECT COUNT(*) FROM mensagens WHERE campanhaId = :campanhaId")
    fun observarTotalMensagens(campanhaId: Long): Flow<Int>

    @Query("SELECT * FROM memorias WHERE campanhaId = :campanhaId ORDER BY ativa DESC, id DESC")
    fun observarMemorias(campanhaId: Long): Flow<List<Memoria>>

    @Query("SELECT * FROM perfil_local WHERE id = 1")
    fun observarPerfil(): Flow<PerfilLocal?>

    @Query("SELECT * FROM preferencias WHERE id = 1")
    fun observarPreferencias(): Flow<Preferencias?>

    @Query("SELECT * FROM personagens WHERE id = :id")
    suspend fun buscarPersonagem(id: Long): Personagem?

    @Query("SELECT * FROM livros WHERE id = :id")
    suspend fun buscarLivro(id: Long): Livro?

    @Query("SELECT * FROM campanhas WHERE id = :id")
    suspend fun buscarCampanha(id: Long): Campanha?

    @Query("SELECT * FROM memorias WHERE id = :id")
    suspend fun buscarMemoria(id: Long): Memoria?

    @Query("SELECT * FROM mensagens WHERE campanhaId = :campanhaId ORDER BY id DESC LIMIT :limite")
    suspend fun buscarMensagensRecentes(campanhaId: Long, limite: Int): List<Mensagem>

    @Query("SELECT * FROM memorias WHERE campanhaId = :campanhaId AND ativa = 1 ORDER BY id")
    suspend fun buscarMemoriasAtivas(campanhaId: Long): List<Memoria>

    @Query("SELECT COUNT(*) FROM campanhas WHERE personagemId = :personagemId")
    suspend fun contarCampanhasDoPersonagem(personagemId: Long): Int

    @Upsert suspend fun atualizarPersonagens(itens: List<Personagem>)
    @Upsert suspend fun atualizarLivros(itens: List<Livro>)
    @Upsert suspend fun atualizarCampanhas(itens: List<Campanha>)
    @Upsert suspend fun atualizarMensagens(itens: List<Mensagem>)
    @Upsert suspend fun atualizarMemorias(itens: List<Memoria>)
    @Query("DELETE FROM memorias WHERE campanhaId = :cid")
    suspend fun limparMemorias(cid: Long)

    @Upsert suspend fun salvarPersonagem(personagem: Personagem): Long
    @Upsert suspend fun salvarMemoria(memoria: Memoria): Long
    @Upsert suspend fun salvarPerfil(perfil: PerfilLocal)
    @Upsert suspend fun salvarPreferencias(preferencias: Preferencias)
    @Insert suspend fun inserirCampanha(campanha: Campanha): Long
    @Insert suspend fun inserirMensagem(mensagem: Mensagem): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserirLivros(livros: List<Livro>)

    @Query("UPDATE campanhas SET ultimaAtividade = :momento WHERE id = :id")
    suspend fun atualizarAtividade(id: Long, momento: Long)

    @Query("DELETE FROM personagens WHERE id = :id")
    suspend fun excluirPersonagem(id: Long)

    @Query("DELETE FROM campanhas WHERE id = :id")
    suspend fun excluirCampanha(id: Long)

    @Query("DELETE FROM memorias WHERE id = :id")
    suspend fun excluirMemoria(id: Long)
}
