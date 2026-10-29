package br.com.monoco.seguranca

import br.com.monoco.conta.*
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.time.Instant
import java.util.Date
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class Tokens(@Value("\${monoco.jwt-secret}") segredo: String) {
    private val chave = Keys.hmacShaKeyFor(segredo.toByteArray(Charsets.UTF_8))
    private val leitor = Jwts.parser().verifyWith(chave).requireIssuer("monoco").build()

    fun emitir(usuario: Usuario): Sessao {
        val inicio = Instant.now()
        val fim = inicio.plusSeconds(7200)
        val token =
            Jwts.builder()
                .header()
                .type("JWT")
                .and()
                .issuer("monoco")
                .subject(usuario.id.toString())
                .issuedAt(Date.from(inicio))
                .expiration(Date.from(fim))
                .claim("versao", usuario.versaoSessao)
                .signWith(chave, Jwts.SIG.HS256)
                .compact()
        return Sessao(token, fim.toEpochMilli(), usuario.paraResposta())
    }

    fun ler(token: String): Claims = leitor.parseSignedClaims(token).payload
}
