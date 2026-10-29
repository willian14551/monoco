package br.com.monoco.narracao

import br.com.monoco.comum.*
import jakarta.annotation.PreDestroy
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

data class MensagemIa(
    @get:com.fasterxml.jackson.annotation.JsonProperty("role") val papel: String,
    @get:com.fasterxml.jackson.annotation.JsonProperty("content") val conteudo: String,
)

data class PedidoNarracao(
    val provedor: String,
    val modelo: String,
    val chave: String,
    val mensagens: List<MensagemIa>,
    val criatividade: Double,
    val tamanho: String,
)

interface ProvedorIa {
    fun testar(provedor: String, modelo: String, chave: String)

    fun narrar(pedido: PedidoNarracao, aoTrecho: (String) -> Unit): String
}

@Component
class ProvedorHttp(private val json: JsonMapper) : ProvedorIa {
    private val cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
    private val temporizador = Executors.newSingleThreadScheduledExecutor()

    @PreDestroy
    fun encerrar() {
        temporizador.shutdownNow()
    }

    private fun obterEnderecoProvedor(provedor: String) =
        when (provedor) {
            "OpenAI" -> "https://api.openai.com/v1"
            "OpenRouter" -> "https://openrouter.ai/api/v1"
            else -> invalido("Provedor inválido.")
        }

    private fun validarStatusDaResposta(status: Int) {
        when {
            status in 200..299 -> return
            status == 401 || status == 403 ->
                throw ErroNegocio(
                    HttpStatus.BAD_GATEWAY,
                    "Chave inválida ou sem permissão no provedor.",
                )
            status == 429 ->
                throw ErroNegocio(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "O provedor atingiu o limite de uso ou de créditos.",
                )
            else ->
                throw ErroNegocio(
                    HttpStatus.BAD_GATEWAY,
                    "O provedor não pôde atender. Confira o modelo e tente novamente.",
                )
        }
    }

    override fun testar(provedor: String, modelo: String, chave: String) {
        if (chave.isBlank() || modelo.isBlank())
            invalido("Informe o modelo e uma chave de API válida.")
        val requisicao =
            HttpRequest.newBuilder(URI.create("${obterEnderecoProvedor(provedor)}/models"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer $chave")
                .GET()
                .build()
        try {
            val resposta = cliente.send(requisicao, HttpResponse.BodyHandlers.ofString())
            validarStatusDaResposta(resposta.statusCode())
            val modelos = json.readTree(resposta.body()).path("data")
            if (modelos.none { it.path("id").asString() == modelo })
                invalido("Modelo não encontrado entre os disponíveis para esta chave.")
        } catch (e: ErroNegocio) {
            throw e
        } catch (_: Exception) {
            throw ErroNegocio(
                HttpStatus.BAD_GATEWAY,
                "Não foi possível validar a chave no provedor.",
            )
        }
    }

    override fun narrar(pedido: PedidoNarracao, aoTrecho: (String) -> Unit): String {
        val limite =
            when (pedido.tamanho) {
                "Curta" -> 350
                "Longa" -> 1200
                else -> 700
            }
        val corpo =
            mutableMapOf<String, Any>(
                "model" to pedido.modelo,
                "messages" to pedido.mensagens,
                "stream" to true,
                "temperature" to pedido.criatividade,
            )
        corpo[if (pedido.provedor == "OpenAI") "max_completion_tokens" else "max_tokens"] = limite
        val requisicao =
            HttpRequest.newBuilder(
                    URI.create("${obterEnderecoProvedor(pedido.provedor)}/chat/completions")
                )
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer ${pedido.chave}")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(corpo)))
                .build()
        val inicio = System.nanoTime()
        try {
            val resposta = cliente.send(requisicao, HttpResponse.BodyHandlers.ofInputStream())
            val fluxo = resposta.body()
            val restante =
                (60_000 - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio)).coerceAtLeast(
                    1
                )
            val prazo = temporizador.schedule({ fluxo.close() }, restante, TimeUnit.MILLISECONDS)
            try {
                validarStatusDaResposta(resposta.statusCode())
                val texto = StringBuilder()
                var concluida = false
                fluxo.bufferedReader(Charsets.UTF_8).use { leitor ->
                    for (linha in leitor.lineSequence()) {
                        if (!linha.startsWith("data:")) continue
                        val dado = linha.substringAfter("data:").trim()
                        if (dado == "[DONE]") {
                            concluida = true
                            break
                        }
                        val item = json.readTree(dado)
                        if (item.has("error"))
                            throw ErroNegocio(
                                HttpStatus.BAD_GATEWAY,
                                "O provedor interrompeu a narração.",
                            )
                        val escolha = item.path("choices").firstOrNull() ?: continue
                        val motivo = escolha.path("finish_reason").asString()
                        if (motivo == "content_filter")
                            throw ErroNegocio(
                                HttpStatus.BAD_GATEWAY,
                                "O provedor recusou esta narração. Reformule a ação.",
                            )
                        if (motivo == "length")
                            throw ErroNegocio(
                                HttpStatus.BAD_GATEWAY,
                                "A resposta excedeu o limite do modelo. Tente novamente com resposta curta.",
                            )
                        val trecho = escolha.path("delta").path("content").asString("")
                        if (trecho.isNotEmpty()) {
                            texto.append(trecho)
                            if (texto.length > 20000)
                                throw ErroNegocio(
                                    HttpStatus.BAD_GATEWAY,
                                    "A narração excedeu o tamanho permitido.",
                                )
                            aoTrecho(trecho)
                        }
                    }
                }
                if (!concluida || texto.isBlank())
                    throw ErroNegocio(
                        HttpStatus.BAD_GATEWAY,
                        "O provedor encerrou a resposta antes de concluir. Tente novamente.",
                    )
                return texto.toString()
            } finally {
                prazo.cancel(false)
                fluxo.close()
            }
        } catch (e: ErroNegocio) {
            throw e
        } catch (_: Exception) {
            throw ErroNegocio(
                HttpStatus.BAD_GATEWAY,
                "Falha ou tempo esgotado no provedor. Sua ação continua salva; tente novamente.",
            )
        }
    }
}
