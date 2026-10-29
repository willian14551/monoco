package br.com.monoco.comum

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import java.net.URI
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.core.annotation.Order
import org.springframework.core.task.TaskRejectedException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

class ErroNegocio(val status: HttpStatus, override val message: String) : RuntimeException(message)

fun naoEncontrado(recurso: String): Nothing =
    throw ErroNegocio(HttpStatus.NOT_FOUND, "$recurso não encontrado.")

fun conflito(mensagem: String): Nothing = throw ErroNegocio(HttpStatus.CONFLICT, mensagem)

fun invalido(mensagem: String): Nothing = throw ErroNegocio(HttpStatus.BAD_REQUEST, mensagem)

fun criarProblema(
    status: HttpStatusCode,
    mensagem: String,
    caminho: String,
    erros: Map<String, List<String>> = emptyMap(),
): ProblemDetail =
    ProblemDetail.forStatusAndDetail(status, mensagem).apply {
        title = HttpStatus.resolve(status.value())?.reasonPhrase ?: "Erro HTTP"
        instance = URI.create(caminho)
        setProperty("errors", erros)
    }

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class TratamentoErros {
    private val registrador = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(Exception::class)
    fun tratar(excecao: Exception, requisicao: HttpServletRequest): ResponseEntity<ProblemDetail> {
        val problema =
            when (excecao) {
                is ErroNegocio ->
                    criarProblema(excecao.status, excecao.message, requisicao.requestURI)
                is MethodArgumentNotValidException ->
                    criarProblema(
                        HttpStatus.BAD_REQUEST,
                        "Confira os campos informados.",
                        requisicao.requestURI,
                        excecao.bindingResult.fieldErrors.groupBy(
                            { it.field },
                            { it.defaultMessage ?: "Valor inválido." },
                        ),
                    )
                is ConstraintViolationException ->
                    criarProblema(
                        HttpStatus.BAD_REQUEST,
                        "Confira os parâmetros informados.",
                        requisicao.requestURI,
                        excecao.constraintViolations.groupBy(
                            { it.propertyPath.toString() },
                            { it.message },
                        ),
                    )
                is MethodArgumentTypeMismatchException ->
                    criarProblema(
                        HttpStatus.BAD_REQUEST,
                        "Parâmetro inválido.",
                        requisicao.requestURI,
                        mapOf(excecao.name to listOf("Confira o tipo do parâmetro.")),
                    )
                is HandlerMethodValidationException ->
                    criarProblema(
                        HttpStatus.BAD_REQUEST,
                        "Confira os parâmetros informados.",
                        requisicao.requestURI,
                        excecao.parameterValidationResults.associate {
                            (it.methodParameter.parameterName ?: "parametro") to
                                it.resolvableErrors.map { erro ->
                                    erro.defaultMessage ?: "Valor inválido."
                                }
                        },
                    )
                is HttpMessageNotReadableException ->
                    criarProblema(
                        HttpStatus.BAD_REQUEST,
                        "O JSON enviado é inválido.",
                        requisicao.requestURI,
                        mapOf("body" to listOf("Confira os tipos e o formato dos campos.")),
                    )
                is DataIntegrityViolationException ->
                    criarProblema(
                        HttpStatus.CONFLICT,
                        "O registro já existe ou está vinculado a outro registro.",
                        requisicao.requestURI,
                    )
                is TaskRejectedException ->
                    criarProblema(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "O narrador está ocupado. Tente novamente em instantes.",
                        requisicao.requestURI,
                    )
                else -> problemaDoSpring(excecao, requisicao)
            }
        return ResponseEntity.status(problema.status)
            .headers(
                if (excecao is ErrorResponse) excecao.headers
                else org.springframework.http.HttpHeaders.EMPTY
            )
            .body(problema)
    }

    private fun problemaDoSpring(
        excecao: Exception,
        requisicao: HttpServletRequest,
    ): ProblemDetail {
        if (excecao is ErrorResponse) {
            val mensagem =
                if (excecao.statusCode.is5xxServerError) "Não foi possível concluir a operação."
                else excecao.body.detail ?: "Dados ou parâmetros inválidos."
            return criarProblema(excecao.statusCode, mensagem, requisicao.requestURI)
        }
        val anotacao =
            AnnotatedElementUtils.findMergedAnnotation(
                excecao.javaClass,
                ResponseStatus::class.java,
            )
        if (anotacao != null)
            return criarProblema(
                anotacao.code,
                anotacao.reason.ifBlank { anotacao.code.reasonPhrase },
                requisicao.requestURI,
            )
        registrador.error(
            "Falha inesperada em {} {}",
            requisicao.method,
            requisicao.requestURI,
            excecao,
        )
        return criarProblema(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Não foi possível concluir a operação.",
            requisicao.requestURI,
        )
    }
}
