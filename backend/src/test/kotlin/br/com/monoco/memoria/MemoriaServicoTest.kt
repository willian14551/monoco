package br.com.monoco.memoria

import br.com.monoco.campanha.Campanha
import br.com.monoco.campanha.CampanhaServico
import br.com.monoco.comum.ErroNegocio
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.http.HttpStatus

class MemoriaServicoTest {
    private lateinit var memorias: MemoriaRepositorio
    private lateinit var campanhas: CampanhaServico
    private lateinit var servico: MemoriaServico
    private val campanha = Campanha(id = 10)

    @BeforeEach
    fun preparar() {
        memorias = mockk()
        campanhas = mockk()
        servico = MemoriaServico(memorias, campanhas)
        every { campanhas.buscarDoUsuario(1, 10) } returns campanha
    }

    @Test
    fun `criar memoria remove espacos e ativa por padrao`() {
        every { memorias.save(any()) } answers { firstArg<Memoria>().apply { id = 20 } }
        val resposta =
            servico.salvar(1, 10, null, MemoriaEntrada(titulo = " Lia ", texto = " Aliada "))
        resposta.titulo shouldBe "Lia"
        resposta.texto shouldBe "Aliada"
        resposta.ativa shouldBe true
    }

    @Test
    fun `edicao pode desativar uma memoria`() {
        val memoria = Memoria(id = 20, campanha = campanha)
        every { memorias.buscarDaCampanha(20, 10) } returns memoria
        every { memorias.save(memoria) } returns memoria
        servico
            .salvar(1, 10, 20, MemoriaEntrada(titulo = "Lia", texto = "Aliada", ativa = false))
            .ativa shouldBe false
    }

    @Test
    fun `dados em branco impedem salvar`() {
        shouldThrow<ErroNegocio> {
                servico.salvar(1, 10, null, MemoriaEntrada(titulo = " ", texto = "Aliada"))
            }
            .status shouldBe HttpStatus.BAD_REQUEST
        verify(exactly = 0) { memorias.save(any()) }
    }

    @Test
    fun `consulta devolve memoria da campanha`() {
        every { memorias.buscarDaCampanha(20, 10) } returns
            Memoria(id = 20, campanha = campanha, titulo = "Lia")
        servico.buscar(1, 10, 20).titulo shouldBe "Lia"
    }

    @Test
    fun `consulta de memoria ausente retorna nao encontrado`() {
        every { memorias.buscarDaCampanha(20, 10) } returns null
        shouldThrow<ErroNegocio> { servico.buscar(1, 10, 20) }.status shouldBe HttpStatus.NOT_FOUND
    }

    @Test
    fun `outra conta nao pode consultar memorias da campanha`() {
        every { campanhas.buscarDoUsuario(2, 10) } throws
            ErroNegocio(HttpStatus.NOT_FOUND, "Campanha não encontrada.")
        shouldThrow<ErroNegocio> { servico.buscar(2, 10, 20) }.status shouldBe HttpStatus.NOT_FOUND
        verify(exactly = 0) { memorias.buscarDaCampanha(any(), any()) }
    }

    @Test
    fun `exclusao remove a memoria da campanha`() {
        val memoria = Memoria(id = 20, campanha = campanha)
        every { memorias.buscarDaCampanha(20, 10) } returns memoria
        every { memorias.delete(memoria) } just Runs
        servico.excluir(1, 10, 20)
        verify { memorias.delete(memoria) }
    }

    @Test
    fun `listagem devolve pagina de memorias`() {
        every { memorias.findAll(any<Specification<Memoria>>(), any<Pageable>()) } returns
            PageImpl(listOf(Memoria(id = 20, campanha = campanha, texto = "Lia é aliada")))
        servico.listar(1, 10, true, 0, 20).conteudo.single().texto shouldBe "Lia é aliada"
    }
}
