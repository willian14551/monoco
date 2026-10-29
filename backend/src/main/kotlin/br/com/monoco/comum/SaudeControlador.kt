package br.com.monoco.comum

import io.swagger.v3.oas.annotations.security.SecurityRequirements
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SaudeControlador {
    @SecurityRequirements
    @GetMapping("/saude")
    fun verificar() = mapOf("status" to "ok", "servico" to "monoco", "versao" to "0.3.0")
}
