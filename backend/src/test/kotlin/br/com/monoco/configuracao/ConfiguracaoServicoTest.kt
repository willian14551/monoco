package br.com.monoco.configuracao

import br.com.monoco.comum.ErroNegocio
import br.com.monoco.narracao.ProvedorIa
import br.com.monoco.seguranca.CifraChaves
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.SimpleTransactionStatus

class ConfiguracaoServicoTest {
    private lateinit var repositorio: ConfiguracaoRepositorio
    private lateinit var provedor: ProvedorIa
    private lateinit var cifra: CifraChaves
    private lateinit var servico: ConfiguracaoServico
    private lateinit var configuracao: ConfiguracaoIa

    @BeforeEach
    fun preparar() {
        repositorio = mockk()
        provedor = mockk()
        cifra = mockk()
        val transacoes = mockk<PlatformTransactionManager>(relaxed = true)
        every { transacoes.getTransaction(any()) } returns SimpleTransactionStatus()
        configuracao =
            ConfiguracaoIa(
                provedor = "OpenAI",
                modelo = "modelo-atual",
                chaveCifrada = "cifrada",
                finalChave = "1234",
            )
        every { repositorio.buscarPorUsuario(1) } returns configuracao
        servico = ConfiguracaoServico(repositorio, cifra, provedor, transacoes, true)
    }

    @Test
    fun `preferencias preservam a conexao existente`() {
        val resposta =
            servico.atualizar(1, ConfiguracaoEntrada(tom = "Sombrio", criatividade = 0.4))
        resposta.tom shouldBe "Sombrio"
        resposta.modelo shouldBe "modelo-atual"
        resposta.finalChave shouldBe "1234"
        verify(exactly = 0) { provedor.testar(any(), any(), any()) }
    }

    @Test
    fun `chave so e salva depois da validacao e cifragem`() {
        every { provedor.testar("OpenAI", "modelo-novo", "chave-nova") } just Runs
        every { cifra.cifrar("chave-nova") } returns "nova-cifra"
        val resposta =
            servico.atualizar(1, ConfiguracaoEntrada(modelo = "modelo-novo", chave = "chave-nova"))
        resposta.modelo shouldBe "modelo-novo"
        configuracao.chaveCifrada shouldBe "nova-cifra"
        resposta.finalChave shouldBe "nova"
    }

    @Test
    fun `falha de validacao mantem tanto a conexao como as preferencias`() {
        every { provedor.testar(any(), any(), any()) } throws
            ErroNegocio(HttpStatus.BAD_GATEWAY, "Chave inválida.")
        shouldThrow<ErroNegocio> {
            servico.atualizar(1, ConfiguracaoEntrada(chave = "invalida", tom = "Sombrio"))
        }
        configuracao.chaveCifrada shouldBe "cifrada"
        configuracao.tom shouldBe "Aventura"
    }

    @Test
    fun `troca de modelo pode reutilizar chave do mesmo provedor`() {
        every { cifra.decifrar("cifrada") } returns "chave-atual"
        every { provedor.testar("OpenAI", "modelo-novo", "chave-atual") } just Runs
        every { cifra.cifrar("chave-atual") } returns "recifrada"
        servico.atualizar(1, ConfiguracaoEntrada(modelo = "modelo-novo")).modelo shouldBe
            "modelo-novo"
    }

    @Test
    fun `troca de provedor exige a chave correspondente`() {
        shouldThrow<ErroNegocio> {
                servico.atualizar(1, ConfiguracaoEntrada(provedor = "OpenRouter"))
            }
            .status shouldBe HttpStatus.BAD_REQUEST
    }

    @Test
    fun `remocao de chave preserva as preferencias`() {
        val resposta = servico.atualizar(1, ConfiguracaoEntrada(removerChave = true))
        resposta.chaveConfigurada shouldBe false
        resposta.tom shouldBe "Aventura"
    }

    @Test
    fun `demonstracao elimina a chave do provedor real`() {
        val resposta = servico.atualizar(1, ConfiguracaoEntrada(provedor = "Demonstracao"))
        resposta.chaveConfigurada shouldBe false
        resposta.modelo shouldBe ""
    }

    @Test
    fun `demonstracao bloqueada nao pode ser ativada`() {
        val transacoes = mockk<PlatformTransactionManager>()
        servico = ConfiguracaoServico(repositorio, cifra, provedor, transacoes, false)
        shouldThrow<ErroNegocio> {
                servico.atualizar(1, ConfiguracaoEntrada(provedor = "Demonstracao"))
            }
            .status shouldBe HttpStatus.BAD_REQUEST
    }

    @Test
    fun `opcoes contraditorias e criatividade invalida sao recusadas`() {
        listOf(
                ConfiguracaoEntrada(removerChave = true, chave = "nova"),
                ConfiguracaoEntrada(criatividade = 2.0),
                ConfiguracaoEntrada(criatividade = Double.NaN),
                ConfiguracaoEntrada(provedor = "Demonstracao", chave = "nova"),
                ConfiguracaoEntrada(chave = " \n"),
            )
            .forEach { dados ->
                shouldThrow<ErroNegocio> { servico.atualizar(1, dados) }.status shouldBe
                    HttpStatus.BAD_REQUEST
            }
    }
}
