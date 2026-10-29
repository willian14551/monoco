package br.com.monoco.configuracao

import br.com.monoco.comum.*
import br.com.monoco.narracao.*
import br.com.monoco.narracao.ProvedorIa
import br.com.monoco.seguranca.*
import br.com.monoco.seguranca.CifraChaves
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@Service
class ConfiguracaoServico(
    private val configuracoes: ConfiguracaoRepositorio,
    private val cifra: CifraChaves,
    private val provedorIa: ProvedorIa,
    gerenciadorTransacoes: PlatformTransactionManager,
    @param:Value("\${monoco.demo-permitida}") val demonstracaoPermitida: Boolean,
) {
    private val transacao = TransactionTemplate(gerenciadorTransacoes)

    fun buscarEntidade(usuarioId: Long) =
        configuracoes.buscarPorUsuario(usuarioId) ?: naoEncontrado("Configuração")

    fun buscar(usuarioId: Long) = buscarEntidade(usuarioId).paraResposta(demonstracaoPermitida)

    fun atualizar(usuarioId: Long, dados: ConfiguracaoEntrada): ConfiguracaoSaida {
        if (
            dados.criatividade != null &&
                (!dados.criatividade.isFinite() || dados.criatividade !in 0.0..1.0)
        )
            invalido("Criatividade deve estar entre 0 e 1.")
        if (
            dados.removerChave == true &&
                (dados.chave != null || dados.provedor != null || dados.modelo != null)
        )
            invalido("Remova a chave separadamente da alteração de provedor, modelo ou chave.")
        val configuracaoAtual = buscarEntidade(usuarioId)
        val alterarConexao = dados.provedor != null || dados.modelo != null || dados.chave != null
        val provedor = dados.provedor ?: configuracaoAtual.provedor
        val modelo = dados.modelo?.trim() ?: configuracaoAtual.modelo
        var chaveValidada: String? = null
        if (alterarConexao) {
            if (provedor == "Demonstracao") {
                if (!demonstracaoPermitida)
                    invalido("O modo de demonstração está desabilitado neste servidor.")
                if (dados.chave != null)
                    invalido("O modo de demonstração não utiliza uma chave de API.")
            } else {
                if (provedor !in setOf("OpenAI", "OpenRouter")) invalido("Provedor inválido.")
                if (
                    dados.provedor != null &&
                        provedor != configuracaoAtual.provedor &&
                        dados.chave == null
                )
                    invalido("Informe a chave do novo provedor.")
                chaveValidada = dados.chave?.trim() ?: obterChave(configuracaoAtual)
                if (chaveValidada.isBlank() || chaveValidada.any { it.isISOControl() })
                    invalido("Informe uma chave válida.")
                provedorIa.testar(provedor, modelo, chaveValidada)
            }
        }
        return transacao
            .execute {
                val configuracao = buscarEntidade(usuarioId)
                dados.tom?.let { configuracao.tom = it }
                dados.tamanhoResposta?.let { configuracao.tamanhoResposta = it }
                dados.criatividade?.let { configuracao.criatividade = it }
                if (alterarConexao) {
                    configuracao.provedor = provedor
                    configuracao.modelo = if (provedor == "Demonstracao") "" else modelo
                    configuracao.chaveCifrada = chaveValidada?.let(cifra::cifrar)
                    configuracao.finalChave = chaveValidada?.takeLast(4)
                }
                if (dados.removerChave == true) {
                    configuracao.chaveCifrada = null
                    configuracao.finalChave = null
                    if (configuracao.provedor == "Demonstracao") configuracao.provedor = "OpenAI"
                }
                configuracao.paraResposta(demonstracaoPermitida)
            }
            .obrigatorio("Configuração atualizada")
    }

    fun obterChave(configuracao: ConfiguracaoIa) =
        configuracao.chaveCifrada?.let(cifra::decifrar)
            ?: invalido("Configure uma chave de API válida nas configurações.")
}
