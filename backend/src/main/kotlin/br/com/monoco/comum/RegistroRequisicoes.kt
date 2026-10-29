package br.com.monoco.comum

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class RegistroRequisicoes : OncePerRequestFilter() {
    private val registrador = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        requisicao: HttpServletRequest,
        resposta: HttpServletResponse,
        cadeia: FilterChain,
    ) {
        val inicio = System.nanoTime()
        try {
            cadeia.doFilter(requisicao, resposta)
        } finally {
            registrador.info(
                "Requisição: metodo={}, caminho={}, usuarioId={}, status={}, duracaoMs={}",
                requisicao.method,
                requisicao.requestURI,
                requisicao.getAttribute("usuarioId"),
                resposta.status,
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio),
            )
        }
    }
}
