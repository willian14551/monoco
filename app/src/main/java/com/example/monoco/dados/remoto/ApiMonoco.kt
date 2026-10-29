package com.example.monoco.dados.remoto

import com.example.monoco.BuildConfig
import com.example.monoco.dados.local.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ErroServidor(val status: Int, mensagem: String) : Exception(mensagem)

data class ConfiguracaoOnline(
    val provedor: String = "",
    val modelo: String = "",
    val chaveConfigurada: Boolean = false,
    val finalChave: String = "",
    val demonstracaoPermitida: Boolean = false,
)

data class HistoricoOnline(val mensagens: List<Mensagem>, val total: Int)

class ApiMonoco(private val cofre: SessaoSegura, private val aoExpirar: () -> Unit) {
    private fun abrir(rota: String, metodo: String): HttpURLConnection {
        val url = URL(cofre.endereco + rota)
        require(url.protocol == "https" || (BuildConfig.DEBUG && url.protocol == "http")) {
            "Use HTTPS para conectar ao servidor."
        }
        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = metodo
            connectTimeout = 10_000
            readTimeout = 80_000
            setRequestProperty("Accept", "application/json")
            cofre.ler()?.token?.let { setRequestProperty("Authorization", "Bearer $it") }
        }
    }

    private fun enviar(conexao: HttpURLConnection, dados: JSONObject?) {
        if (dados != null) {
            conexao.doOutput = true
            conexao.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
            conexao.outputStream.use { it.write(dados.toString().toByteArray(Charsets.UTF_8)) }
        }
    }

    private fun verificar(conexao: HttpURLConnection) {
        val status = conexao.responseCode
        if (status !in 200..299) {
            if (status == 401) {
                cofre.limpar()
                aoExpirar()
            }
            val erro =
                conexao.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val msg =
                runCatching {
                        val corpo = JSONObject(erro)
                        val campos = corpo.optJSONObject("errors")
                        val detalhes =
                            campos
                                ?.keys()
                                ?.asSequence()
                                ?.joinToString("\n") {
                                    "${it}: ${campos.optJSONArray(it)?.let { lista -> (0 until lista.length()).joinToString("; ") { indice -> lista.getString(indice) } } ?: campos.optString(it)}"
                                }
                                .orEmpty()
                        corpo.optString("detail", "Não foi possível concluir a operação.") +
                            (if (detalhes.isBlank()) "" else "\n$detalhes")
                    }
                    .getOrDefault("Não foi possível concluir a operação.")
            throw ErroServidor(status, msg)
        }
    }

    suspend fun chamar(
        rota: String,
        metodo: String = "GET",
        dados: JSONObject? = null,
    ): JSONObject =
        withContext(Dispatchers.IO) {
            val conexao = abrir(rota, metodo)
            try {
                enviar(conexao, dados)
                verificar(conexao)
                if (conexao.responseCode == 204) JSONObject()
                else
                    JSONObject(
                        conexao.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    )
            } catch (e: java.io.IOException) {
                throw Exception(
                    "Não foi possível conectar. Confira dados endereço e se dados backend está ligado.",
                    e,
                )
            } finally {
                conexao.disconnect()
            }
        }

    suspend fun cadastrar(nome: String, email: String, senha: String) {
        chamar(
            "usuarios",
            "POST",
            JSONObject().put("nome", nome.trim()).put("email", email.trim()).put("senha", senha),
        )
    }

    suspend fun entrar(email: String, senha: String): SessaoOnline {
        val resposta =
            chamar("sessoes", "POST", JSONObject().put("email", email.trim()).put("senha", senha))
        val u = resposta.getJSONObject("usuario")
        return SessaoOnline(
                resposta.getString("token"),
                resposta.getLong("expiraEm"),
                ContaOnline(u.getLong("id"), u.getString("nome"), u.getString("email")),
            )
            .also { cofre.salvar(it) }
    }

    suspend fun sair() {
        try {
            chamar("sessoes", "DELETE", JSONObject())
        } finally {
            cofre.limpar()
        }
    }

    suspend fun perfil() = chamar("perfil")

    suspend fun salvarPerfil(nome: String, email: String) =
        chamar("perfil", "PUT", JSONObject().put("nome", nome).put("email", email))

    suspend fun alterarSenha(atual: String, nova: String) {
        chamar("perfil/senha", "PUT", JSONObject().put("senhaAtual", atual).put("novaSenha", nova))
        cofre.limpar()
        aoExpirar()
    }

    suspend fun configuracao() = chamar("configuracao")

    suspend fun salvarPreferencias(preferencias: Preferencias) =
        chamar(
            "configuracao",
            "PATCH",
            JSONObject()
                .put("tom", preferencias.tom)
                .put("tamanhoResposta", preferencias.tamanhoResposta)
                .put("criatividade", preferencias.criatividade.toDouble()),
        )

    suspend fun configurarProvedor(provedor: String, modelo: String, chave: String) =
        chamar(
            "configuracao",
            "PATCH",
            JSONObject().put("provedor", provedor).put("modelo", modelo).apply {
                if (provedor != "Demonstracao") put("chave", chave)
            },
        )

    suspend fun removerChave() =
        chamar("configuracao", "PATCH", JSONObject().put("removerChave", true))

    private suspend fun <T> listarTudo(rota: String, mapear: (JSONObject) -> T): List<T> {
        val itens = mutableListOf<T>()
        var pagina = 0
        do {
            val resposta = chamar("$rota${if('?' in rota) '&' else '?'}pagina=$pagina&tamanho=100")
            val lista = resposta.getJSONArray("conteudo")
            for (i in 0 until lista.length()) itens += mapear(lista.getJSONObject(i))
            pagina++
        } while (!resposta.getBoolean("ultima"))
        return itens
    }

    suspend fun personagens() = listarTudo("personagens", ::personagem)

    suspend fun livros() = listarTudo("livros", ::livro)

    suspend fun campanhas() = listarTudo("campanhas", ::campanha)

    suspend fun memorias(campanhaId: Long) = listarTudo("campanhas/$campanhaId/memorias", ::memoria)

    suspend fun salvarPersonagem(personagem: Personagem) {
        val preferencias =
            JSONObject()
                .put("nome", personagem.nome)
                .put("classe", personagem.classe)
                .put("atributos", personagem.atributos)
                .put("historia", personagem.historia)
                .put("segredos", personagem.segredos)
                .put("compartilharNome", personagem.compartilharNome)
                .put("compartilharClasse", personagem.compartilharClasse)
                .put("compartilharAtributos", personagem.compartilharAtributos)
                .put("compartilharHistoria", personagem.compartilharHistoria)
                .put("compartilharSegredos", personagem.compartilharSegredos)
        chamar(
            if (personagem.id == 0L) "personagens" else "personagens/${personagem.id}",
            if (personagem.id == 0L) "POST" else "PUT",
            preferencias,
        )
    }

    suspend fun excluirPersonagem(id: Long) {
        chamar("personagens/$id", "DELETE")
    }

    suspend fun criarCampanha(titulo: String, personagemId: Long, livroId: Long) =
        chamar(
                "campanhas",
                "POST",
                JSONObject()
                    .put("titulo", titulo)
                    .put("personagemId", personagemId)
                    .put("livroId", livroId),
            )
            .getLong("id")

    suspend fun excluirCampanha(id: Long) {
        chamar("campanhas/$id", "DELETE")
    }

    suspend fun salvarMemoria(memoria: Memoria) {
        chamar(
            "campanhas/${memoria.campanhaId}/memorias" +
                if (memoria.id == 0L) "" else "/${memoria.id}",
            if (memoria.id == 0L) "POST" else "PUT",
            JSONObject()
                .put("titulo", memoria.titulo)
                .put("texto", memoria.texto)
                .put("ativa", memoria.ativa),
        )
    }

    suspend fun excluirMemoria(campanhaId: Long, id: Long) {
        chamar("campanhas/$campanhaId/memorias/$id", "DELETE")
    }

    suspend fun historico(campanhaId: Long, antesDe: Long? = null): HistoricoOnline {
        val resposta =
            chamar(
                "campanhas/$campanhaId/mensagens?limite=30" +
                    (antesDe?.let { "&antesDe=$it" } ?: "")
            )
        val lista = resposta.getJSONArray("mensagens")
        return HistoricoOnline(
            (0 until lista.length()).map { mensagem(lista.getJSONObject(it)) },
            resposta.getInt("total"),
        )
    }

    suspend fun enviarAcao(
        campanhaId: Long,
        texto: String,
        requisicaoId: String = UUID.randomUUID().toString(),
    ): Mensagem =
        mensagem(
            chamar(
                "campanhas/$campanhaId/mensagens",
                "POST",
                JSONObject().put("texto", texto).put("requisicaoId", requisicaoId),
            )
        )

    suspend fun narrar(campanhaId: Long, aoTrecho: (String) -> Unit): Mensagem =
        withContext(Dispatchers.IO) {
            val contextoCorrotina = currentCoroutineContext()
            val conexao = abrir("campanhas/$campanhaId/narracoes", "POST")
            try {
                conexao.setRequestProperty("Accept", "text/event-stream")
                enviar(conexao, JSONObject())
                verificar(conexao)
                var evento = ""
                val dados = StringBuilder()
                var resposta: Mensagem? = null
                conexao.inputStream.bufferedReader(Charsets.UTF_8).use { leitor ->
                    fun processar() {
                        if (dados.isEmpty()) return
                        val json = JSONObject(dados.toString())
                        when (evento) {
                            "trecho" -> aoTrecho(json.getString("texto"))
                            "concluida" -> resposta = mensagem(json)
                            "erro" ->
                                throw ErroServidor(json.getInt("status"), json.getString("detail"))
                        }
                        dados.clear()
                        evento = ""
                    }
                    while (true) {
                        contextoCorrotina.ensureActive()
                        val linha = leitor.readLine() ?: break
                        when {
                            linha.isEmpty() -> processar()
                            linha.startsWith("event:") -> evento = linha.substringAfter(':').trim()
                            linha.startsWith("data:") -> {
                                if (dados.isNotEmpty()) dados.append('\n')
                                dados.append(linha.substringAfter(':').trimStart())
                            }
                        }
                    }
                    processar()
                }
                resposta
                    ?: throw Exception(
                        "A conexão foi interrompida. Atualize dados histórico ou tente novamente."
                    )
            } finally {
                conexao.disconnect()
            }
        }

    private fun personagem(dados: JSONObject) =
        Personagem(
            dados.getLong("id"),
            dados.getString("nome"),
            dados.getString("classe"),
            dados.getString("atributos"),
            dados.getString("historia"),
            dados.getString("segredos"),
            dados.getBoolean("compartilharNome"),
            dados.getBoolean("compartilharClasse"),
            dados.getBoolean("compartilharAtributos"),
            dados.getBoolean("compartilharHistoria"),
            dados.getBoolean("compartilharSegredos"),
        )

    private fun livro(dados: JSONObject) =
        Livro(
            dados.getLong("id"),
            dados.getString("titulo"),
            dados.getString("genero"),
            dados.getString("descricao"),
            dados.getString("premissa"),
            dados.getString("regras"),
            dados.getString("abertura"),
            dados.getString("duracao"),
        )

    private fun campanha(dados: JSONObject) =
        Campanha(
            dados.getLong("id"),
            dados.getString("titulo"),
            dados.getLong("personagemId"),
            dados.getLong("livroId"),
            dados.getLong("ultimaAtividade"),
        )

    private fun memoria(dados: JSONObject) =
        Memoria(
            dados.getLong("id"),
            dados.getLong("campanhaId"),
            dados.getString("titulo"),
            dados.getString("texto"),
            dados.getBoolean("ativa"),
        )

    private fun mensagem(dados: JSONObject) =
        Mensagem(
            dados.getLong("id"),
            dados.getLong("campanhaId"),
            dados.getString("autor"),
            dados.getString("texto"),
            dados.getLong("criadaEm"),
            dados.getBoolean("interrompida"),
        )
}
