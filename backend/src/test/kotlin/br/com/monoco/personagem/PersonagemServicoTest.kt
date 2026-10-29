package br.com.monoco.personagem

import br.com.monoco.campanha.CampanhaRepositorio
import br.com.monoco.comum.ErroNegocio
import br.com.monoco.conta.ContaServico
import br.com.monoco.conta.Usuario
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.http.HttpStatus

class PersonagemServicoTest {
    private lateinit var personagens: PersonagemRepositorio
    private lateinit var campanhas: CampanhaRepositorio
    private lateinit var contas: ContaServico
    private lateinit var servico: PersonagemServico
    private val usuario = Usuario(id = 1, nome = "Willian")

    @BeforeEach
    fun preparar() {
        personagens = mockk()
        campanhas = mockk()
        contas = mockk()
        servico = PersonagemServico(personagens, campanhas, contas)
    }

    @Test
    fun `criar ficha aplica os valores padrao e respeita a visibilidade`() {
        every { contas.buscar(1) } returns usuario
        every { personagens.save(any()) } answers { firstArg<Personagem>().apply { id = 10 } }
        val resposta =
            servico.salvar(
                1,
                null,
                PersonagemEntrada(
                    nome = " Falken ",
                    segredos = "Juramento",
                    compartilharSegredos = false,
                ),
            )
        resposta.nome shouldBe "Falken"
        resposta.compartilharNome shouldBe true
        resposta.compartilharSegredos shouldBe false
        resposta.classe shouldBe ""
    }

    @Test
    fun `nome em branco impede salvar a ficha`() {
        every { contas.buscar(1) } returns usuario
        shouldThrow<ErroNegocio> { servico.salvar(1, null, PersonagemEntrada(nome = " ")) }
            .status shouldBe HttpStatus.BAD_REQUEST
        verify(exactly = 0) { personagens.save(any()) }
    }

    @Test
    fun `consulta de ficha inexistente retorna nao encontrado`() {
        every { personagens.buscarDoUsuario(10, 1) } returns null
        shouldThrow<ErroNegocio> { servico.buscar(1, 10) }.status shouldBe HttpStatus.NOT_FOUND
    }

    @Test
    fun `consulta devolve a ficha do proprietario`() {
        every { personagens.buscarDoUsuario(10, 1) } returns
            Personagem(id = 10, usuario = usuario, nome = "Falken")
        servico.buscar(1, 10).nome shouldBe "Falken"
    }

    @Test
    fun `edicao atualiza a ficha existente`() {
        val personagem = Personagem(id = 10, usuario = usuario, nome = "Anterior")
        every { personagens.bloquearParaAlteracao(10, 1) } returns personagem
        every { personagens.save(personagem) } returns personagem
        servico
            .salvar(1, 10, PersonagemEntrada(nome = "Novo", classe = " Paladino "))
            .classe shouldBe "Paladino"
    }

    @Test
    fun `edicao de ficha ausente retorna nao encontrado`() {
        every { personagens.bloquearParaAlteracao(10, 1) } returns null
        shouldThrow<ErroNegocio> { servico.salvar(1, 10, PersonagemEntrada(nome = "Novo")) }
            .status shouldBe HttpStatus.NOT_FOUND
    }

    @Test
    fun `exclusao de ficha vinculada e recusada`() {
        every { personagens.bloquearParaAlteracao(10, 1) } returns Personagem(id = 10)
        every { campanhas.existeParaPersonagem(10) } returns true
        shouldThrow<ErroNegocio> { servico.excluir(1, 10) }.status shouldBe HttpStatus.CONFLICT
        verify(exactly = 0) { personagens.delete(any<Personagem>()) }
    }

    @Test
    fun `exclusao remove ficha sem campanhas`() {
        val personagem = Personagem(id = 10)
        every { personagens.bloquearParaAlteracao(10, 1) } returns personagem
        every { campanhas.existeParaPersonagem(10) } returns false
        every { personagens.delete(personagem) } just Runs
        servico.excluir(1, 10)
        verify { personagens.delete(personagem) }
    }

    @Test
    fun `exclusao de ficha ausente retorna nao encontrado`() {
        every { personagens.bloquearParaAlteracao(10, 1) } returns null
        shouldThrow<ErroNegocio> { servico.excluir(1, 10) }.status shouldBe HttpStatus.NOT_FOUND
    }

    @Test
    fun `listagem preserva os dados de paginacao`() {
        every { personagens.findAll(any<Specification<Personagem>>(), any<Pageable>()) } returns
            PageImpl(listOf(Personagem(id = 10, nome = "Falken")))
        servico.listar(1, "Fal", 0, 20, "nome", "asc").conteudo.single().nome shouldBe "Falken"
    }
}
