package br.com.monoco.seguranca

import br.com.monoco.comum.criarProblema
import br.com.monoco.conta.*
import br.com.monoco.conta.UsuarioRepositorio
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.MalformedJwtException
import jakarta.servlet.DispatcherType
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.stereotype.Component
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.json.JsonMapper

@Component
class FiltroToken(
    private val tokens: Tokens,
    private val usuarios: UsuarioRepositorio,
    private val json: JsonMapper,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        requisicao: HttpServletRequest,
        resposta: HttpServletResponse,
        cadeia: FilterChain,
    ) {
        val cabecalho = requisicao.getHeader("Authorization")
        if (cabecalho != null) {
            try {
                if (!cabecalho.startsWith("Bearer "))
                    throw MalformedJwtException("Tipo de token inválido")
                val dadosToken = tokens.ler(cabecalho.substring(7))
                val usuarioId =
                    dadosToken.subject?.toLongOrNull()
                        ?: throw MalformedJwtException("Identificador inválido")
                val usuario = usuarios.findById(usuarioId).orElse(null)
                if (
                    usuario == null ||
                        (dadosToken["versao"] as? Number)?.toInt() != usuario.versaoSessao
                )
                    throw MalformedJwtException("Sessão encerrada")
                SecurityContextHolder.getContext().authentication =
                    UsernamePasswordAuthenticationToken(
                        usuarioId,
                        null,
                        listOf(SimpleGrantedAuthority("ROLE_USUARIO")),
                    )
                requisicao.setAttribute("usuarioId", usuarioId)
            } catch (_: JwtException) {
                resposta.status = 401
                resposta.contentType = "application/problem+json;charset=UTF-8"
                json.writeValue(
                    resposta.outputStream,
                    criarProblema(
                        HttpStatus.UNAUTHORIZED,
                        "Sessão inválida ou expirada. Entre novamente.",
                        requisicao.requestURI,
                    ),
                )
                return
            }
        }
        cadeia.doFilter(requisicao, resposta)
    }
}

@Configuration
class ConfiguracaoSeguranca(
    private val filtro: FiltroToken,
    private val json: JsonMapper,
    @param:Value("\${monoco.cors-origens}") private val origens: List<String>,
) {
    @Bean fun senhas() = BCryptPasswordEncoder()

    @Bean fun registroFiltroToken() = FilterRegistrationBean(filtro).apply { isEnabled = false }

    @Bean
    fun configurarCors() =
        UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration(
                "/**",
                CorsConfiguration().apply {
                    allowedOrigins = origens
                    allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                    allowedHeaders = listOf("Authorization", "Content-Type", "Accept")
                    exposedHeaders = listOf("Location")
                    allowCredentials = false
                },
            )
        }

    @Bean
    fun configurarRotas(http: HttpSecurity) =
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(configurarCors()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR)
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/usuarios", "/sessoes")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/saude",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/",
                    )
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }
            .exceptionHandling {
                it.authenticationEntryPoint { requisicao, resposta, _ ->
                    resposta.status = 401
                    resposta.contentType = "application/problem+json;charset=UTF-8"
                    json.writeValue(
                        resposta.outputStream,
                        criarProblema(
                            HttpStatus.UNAUTHORIZED,
                            "Entre para acessar este recurso.",
                            requisicao.requestURI,
                        ),
                    )
                }
                it.accessDeniedHandler { requisicao, resposta, _ ->
                    resposta.status = 403
                    resposta.contentType = "application/problem+json;charset=UTF-8"
                    json.writeValue(
                        resposta.outputStream,
                        criarProblema(HttpStatus.FORBIDDEN, "Acesso negado.", requisicao.requestURI),
                    )
                }
            }
            .addFilterBefore(filtro, UsernamePasswordAuthenticationFilter::class.java)
            .build()
}
