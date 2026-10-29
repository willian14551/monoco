package br.com.monoco.campanha

import br.com.monoco.narracao.*
import br.com.monoco.narracao.ChatTransacoes
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@Validated
@RequestMapping("/campanhas")
class CampanhaControlador(
    private val campanhas: CampanhaServico,
    private val mensagens: MensagemServico,
    private val transacoesChat: ChatTransacoes,
) {
    @GetMapping
    fun listar(
        @AuthenticationPrincipal usuarioId: Long,
        @RequestParam(defaultValue = "") busca: String,
        @RequestParam(defaultValue = "0") pagina: Int,
        @RequestParam(defaultValue = "50") tamanho: Int,
        @RequestParam(defaultValue = "ultimaAtividade") ordenarPor: String,
        @RequestParam(defaultValue = "desc") direcao: String,
    ) = campanhas.listar(usuarioId, busca, pagina, tamanho, ordenarPor, direcao)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun criar(
        @AuthenticationPrincipal usuarioId: Long,
        @Valid @RequestBody dados: CampanhaEntrada,
    ) = campanhas.criar(usuarioId, dados)

    @DeleteMapping("/{campanhaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun excluir(
        @AuthenticationPrincipal usuarioId: Long,
        @PathVariable @Positive campanhaId: Long,
    ) = campanhas.excluir(usuarioId, campanhaId)

    @GetMapping("/{campanhaId}/mensagens")
    fun listarMensagens(
        @AuthenticationPrincipal usuarioId: Long,
        @PathVariable @Positive campanhaId: Long,
        @RequestParam(required = false) antesDe: Long?,
        @RequestParam(defaultValue = "30") limite: Int,
    ) = mensagens.listar(usuarioId, campanhaId, antesDe, limite)

    @PostMapping("/{campanhaId}/mensagens")
    @ResponseStatus(HttpStatus.CREATED)
    fun salvarAcao(
        @AuthenticationPrincipal usuarioId: Long,
        @PathVariable @Positive campanhaId: Long,
        @Valid @RequestBody dados: AcaoEntrada,
    ) = transacoesChat.salvarAcao(usuarioId, campanhaId, dados)
}
