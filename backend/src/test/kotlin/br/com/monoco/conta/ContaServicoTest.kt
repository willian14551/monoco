package br.com.monoco.conta

import br.com.monoco.comum.ErroNegocio
import br.com.monoco.configuracao.ConfiguracaoRepositorio
import br.com.monoco.seguranca.Tokens
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.util.Optional
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

class ContaServicoTest {
    private lateinit var usuarios: UsuarioRepositorio
    private lateinit var configuracoes: ConfiguracaoRepositorio
    private lateinit var tokens: Tokens
    private lateinit var servico: ContaServico
    private val senhas = BCryptPasswordEncoder(4)

    private fun usuario() =
        Usuario(
            id = 1,
            nome = "Willian",
            email = "willian@exemplo.com",
            senhaHash = senhas.encode("senha12345")!!,
        )

    @BeforeEach
    fun preparar() {
        usuarios = mockk()
        configuracoes = mockk()
        every { configuracoes.save(any<br.com.monoco.configuracao.ConfiguracaoIa>()) } answers
            {
                firstArg<br.com.monoco.configuracao.ConfiguracaoIa>()
            }
        tokens = mockk()
        servico = ContaServico(usuarios, configuracoes, senhas, tokens, true)
    }

    @Test
    fun `cadastro normaliza o perfil e protege a senha`() {
        every { usuarios.existePorEmail("willian@exemplo.com") } returns false
        every { usuarios.save(any()) } answers { firstArg<Usuario>().apply { id = 1 } }
        servico.cadastrar(Cadastro(" Willian ", "WILLIAN@exemplo.com", "senha12345")).email shouldBe
            "willian@exemplo.com"
        verify {
            configuracoes.save(
                match { it.provedor == "Demonstracao" && it.usuario.senhaHash != "senha12345" }
            )
        }
    }

    @Test
    fun `cadastro sem demonstracao solicita provedor real`() {
        servico = ContaServico(usuarios, configuracoes, senhas, tokens, false)
        every { usuarios.existePorEmail(any()) } returns false
        every { usuarios.save(any()) } answers { firstArg<Usuario>().apply { id = 1 } }
        servico.cadastrar(Cadastro("Willian", "willian@exemplo.com", "senha12345"))
        verify { configuracoes.save(match { it.provedor == "OpenAI" }) }
    }

    @Test
    fun `senha curta ou maior que o limite em bytes e recusada`() {
        listOf("123", " ".repeat(8), "á".repeat(40)).forEach { senha ->
            shouldThrow<ErroNegocio> {
                    servico.cadastrar(Cadastro("Willian", "willian@exemplo.com", senha))
                }
                .status shouldBe HttpStatus.BAD_REQUEST
        }
    }

    @Test
    fun `nome invalido impede cadastro`() {
        shouldThrow<ErroNegocio> {
                servico.cadastrar(Cadastro(" ", "willian@exemplo.com", "senha12345"))
            }
            .status shouldBe HttpStatus.BAD_REQUEST
    }

    @Test
    fun `email duplicado impede cadastro`() {
        every { usuarios.existePorEmail(any()) } returns true
        shouldThrow<ErroNegocio> {
                servico.cadastrar(Cadastro("Willian", "willian@exemplo.com", "senha12345"))
            }
            .status shouldBe HttpStatus.CONFLICT
    }

    @Test
    fun `login valido emite uma sessao`() {
        val usuario = usuario()
        every { usuarios.buscarPorEmail(usuario.email) } returns usuario
        every { tokens.emitir(usuario) } returns Sessao("token", 1000, usuario.paraResposta())
        servico.entrar(Login(usuario.email, "senha12345")).token shouldBe "token"
    }

    @Test
    fun `login desconhecido senha errada ou excesso de bytes retornam o mesmo erro`() {
        every { usuarios.buscarPorEmail("willian@exemplo.com") } returns usuario()
        every { usuarios.buscarPorEmail("outra@exemplo.com") } returns null
        listOf(
                Login("outra@exemplo.com", "senha12345"),
                Login("willian@exemplo.com", "incorreta"),
                Login("willian@exemplo.com", "á".repeat(40)),
            )
            .forEach { dados ->
                val erro = shouldThrow<ErroNegocio> { servico.entrar(dados) }
                erro.status shouldBe HttpStatus.UNAUTHORIZED
                erro.message shouldBe "E-mail ou senha incorretos."
            }
    }

    @Test
    fun `consulta de usuario ausente retorna nao encontrado`() {
        every { usuarios.findById(1) } returns Optional.empty()
        shouldThrow<ErroNegocio> { servico.buscar(1) }.status shouldBe HttpStatus.NOT_FOUND
    }

    @Test
    fun `perfil permite manter o email da propria conta`() {
        val usuario = usuario()
        every { usuarios.findById(1) } returns Optional.of(usuario)
        every { usuarios.buscarPorEmail(usuario.email) } returns usuario
        servico.editar(1, PerfilEntrada(" Aurel ", usuario.email)).nome shouldBe "Aurel"
    }

    @Test
    fun `perfil pode usar email novo mas recusa email de outra conta`() {
        every { usuarios.findById(1) } returns Optional.of(usuario())
        every { usuarios.buscarPorEmail("novo@exemplo.com") } returns null
        servico.editar(1, PerfilEntrada("Willian", "novo@exemplo.com")).email shouldBe
            "novo@exemplo.com"
        every { usuarios.buscarPorEmail("usado@exemplo.com") } returns Usuario(id = 2)
        shouldThrow<ErroNegocio> {
                servico.editar(1, PerfilEntrada("Willian", "usado@exemplo.com"))
            }
            .status shouldBe HttpStatus.CONFLICT
    }

    @Test
    fun `perfil recusa nome invalido`() {
        shouldThrow<ErroNegocio> { servico.editar(1, PerfilEntrada(" ", "willian@exemplo.com")) }
            .status shouldBe HttpStatus.BAD_REQUEST
    }

    @Test
    fun `alterar senha encerra as sessoes existentes`() {
        val usuario = usuario()
        every { usuarios.findById(1) } returns Optional.of(usuario)
        servico.alterarSenha(1, SenhaEntrada("senha12345", "novaSenha12345"))
        senhas.matches("novaSenha12345", usuario.senhaHash) shouldBe true
        usuario.versaoSessao shouldBe 1
    }

    @Test
    fun `senha atual incorreta ou longa nao altera a conta`() {
        val usuario = usuario()
        every { usuarios.findById(1) } returns Optional.of(usuario)
        listOf("incorreta", "á".repeat(40)).forEach { senha ->
            shouldThrow<ErroNegocio> {
                    servico.alterarSenha(1, SenhaEntrada(senha, "novaSenha12345"))
                }
                .status shouldBe HttpStatus.BAD_REQUEST
        }
        usuario.versaoSessao shouldBe 0
    }

    @Test
    fun `logout invalida as sessoes da conta`() {
        val usuario = usuario()
        every { usuarios.findById(1) } returns Optional.of(usuario)
        servico.sair(1)
        usuario.versaoSessao shouldBe 1
    }
}
