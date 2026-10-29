package br.com.monoco.narracao

import br.com.monoco.campanha.*
import br.com.monoco.campanha.CampanhaServico
import br.com.monoco.comum.*
import java.util.concurrent.atomic.AtomicBoolean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
class NarracaoControlador(
    private val campanhas: CampanhaServico,
    private val chat: ChatServico,
    private val tarefasNarracao: ThreadPoolTaskExecutor,
) {
    @PostMapping(
        "/campanhas/{campanhaId}/narracoes",
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE],
    )
    fun narrarProgressivamente(
        @AuthenticationPrincipal usuarioId: Long,
        @PathVariable campanhaId: Long,
    ): SseEmitter {
        campanhas.buscarDoUsuario(usuarioId, campanhaId)
        val emissor = SseEmitter(75_000L)
        val conectado = AtomicBoolean(true)
        emissor.onCompletion { conectado.set(false) }
        emissor.onTimeout { conectado.set(false) }
        emissor.onError { conectado.set(false) }
        fun evento(nome: String, dados: Any) {
            if (conectado.get())
                try {
                    emissor.send(SseEmitter.event().name(nome).data(dados))
                } catch (_: Exception) {
                    conectado.set(false)
                }
        }
        tarefasNarracao.execute {
            try {
                val resposta =
                    chat.gerar(usuarioId, campanhaId) { evento("trecho", mapOf("texto" to it)) }
                evento("concluida", resposta)
            } catch (e: ErroNegocio) {
                evento(
                    "erro",
                    criarProblema(e.status, e.message, "/api/campanhas/$campanhaId/narracoes"),
                )
            } catch (_: Exception) {
                evento(
                    "erro",
                    criarProblema(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Não foi possível concluir a narração. Sua ação continua salva.",
                        "/api/campanhas/$campanhaId/narracoes",
                    ),
                )
            } finally {
                if (conectado.get()) emissor.complete()
            }
        }
        return emissor
    }
}

@Configuration
class TarefasChat {
    @Bean
    fun tarefasNarracao() =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = 2
            maxPoolSize = 4
            queueCapacity = 20
            setThreadNamePrefix("narracao-")
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(65)
        }
}
