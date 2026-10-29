package br.com.monoco.comum

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.server.ResponseStatusException

class TratamentoErrosTest {
    private val tratamento = TratamentoErros()
    private val requisicao = MockHttpServletRequest("GET", "/api/personagens")

    @ResponseStatus(HttpStatus.CONFLICT, reason = "Operação em conflito.")
    class OperacaoEmConflito : RuntimeException()

    @Test
    fun `anotacao ResponseStatus continua sendo respeitada`() {
        val resposta = tratamento.tratar(OperacaoEmConflito(), requisicao)
        resposta.statusCode shouldBe HttpStatus.CONFLICT
        resposta.body?.detail shouldBe "Operação em conflito."
    }

    @Test
    fun `ResponseStatusException preserva o status e o motivo`() {
        val resposta =
            tratamento.tratar(
                ResponseStatusException(HttpStatus.NOT_FOUND, "Recurso ausente."),
                requisicao,
            )
        resposta.statusCode shouldBe HttpStatus.NOT_FOUND
        resposta.body?.detail shouldBe "Recurso ausente."
    }

    @Test
    fun `erro inesperado devolve mensagem generica sem a excecao`() {
        val resposta =
            tratamento.tratar(IllegalStateException("detalhe interno de teste"), requisicao)
        resposta.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
        resposta.body?.detail shouldBe "Não foi possível concluir a operação."
        resposta.body?.properties?.get("errors") shouldBe emptyMap<String, List<String>>()
    }
}
