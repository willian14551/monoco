package br.com.monoco

import br.com.monoco.campanha.*
import br.com.monoco.comum.*
import br.com.monoco.configuracao.*
import br.com.monoco.conta.*
import br.com.monoco.livro.*
import br.com.monoco.memoria.*
import br.com.monoco.narracao.*
import br.com.monoco.personagem.*
import br.com.monoco.seguranca.*
import java.net.URI
import java.util.UUID
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

@SpringBootTest(
    properties =
        [
            "spring.profiles.active=test",
            "spring.datasource.url=jdbc:h2:mem:monoco-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "monoco.jwt-secret=chave-secreta-exclusiva-de-testes-com-32-bytes",
            "monoco.encryption-key=bW9ub2NvLWRldi1jaGF2ZS0zMi1ieXRlcy0xMjM0NTY=",
            "monoco.demo-permitida=true",
        ]
)
@AutoConfigureMockMvc
class ApiIntegracaoTest {
    @Autowired lateinit var simuladorHttp: MockMvc
    @Autowired lateinit var conversorJson: JsonMapper
    @Autowired lateinit var provedorSimulado: ProvedorFake
    @Autowired lateinit var configuracoes: ConfiguracaoRepositorio
    @Autowired lateinit var usuarios: UsuarioRepositorio
    @Autowired lateinit var mensagens: MensagemRepositorio
    @Autowired lateinit var memorias: MemoriaRepositorio
    @Autowired lateinit var campanhas: CampanhaRepositorio
    @Autowired lateinit var cifra: CifraChaves

    @TestConfiguration
    class Dependencias {
        @Bean @Primary fun provedorSimulado() = ProvedorFake()
    }

    class ProvedorFake : ProvedorIa {
        var falhar = false
        var ultimo: PedidoNarracao? = null
        var chamadas = 0

        override fun testar(provedor: String, modelo: String, chave: String) {
            if (chave == "invalida") invalido("Chave inválida.")
        }

        override fun narrar(pedido: PedidoNarracao, aoTrecho: (String) -> Unit): String {
            chamadas++
            ultimo = pedido
            if (falhar) throw ErroNegocio(HttpStatus.BAD_GATEWAY, "Provedor indisponível.")
            aoTrecho("Uma luz ")
            aoTrecho("se acende.")
            return "Uma luz se acende."
        }
    }

    companion object {
        private val sequencia = java.util.concurrent.atomic.AtomicLong(1)
    }

    @BeforeEach
    fun resetar() {
        provedorSimulado.falhar = false
        provedorSimulado.ultimo = null
        provedorSimulado.chamadas = 0
    }

    data class Jogador(val token: String, val id: Long, val email: String)

    private fun chamar(
        m: String,
        rota: String,
        token: String? = null,
        dados: Any? = null,
        status: Int = 200,
    ): JsonNode {
        val pedido =
            MockMvcRequestBuilders.request(HttpMethod.valueOf(m), URI.create("/api$rota"))
                .contextPath("/api")
        if (token != null) pedido.header("Authorization", "Bearer $token")
        if (dados != null)
            pedido
                .contentType(MediaType.APPLICATION_JSON)
                .content(conversorJson.writeValueAsBytes(dados))
        val r =
            simuladorHttp
                .perform(pedido)
                .andExpect(MockMvcResultMatchers.status().`is`(status))
                .andReturn()
                .response
        return if (r.contentAsString.isBlank()) conversorJson.createObjectNode()
        else conversorJson.readTree(r.contentAsByteArray)
    }

    private fun conta(): Jogador {
        val email = "jogador${sequencia.getAndIncrement()}@exemplo.com"
        val u =
            chamar(
                "POST",
                "/usuarios",
                dados = mapOf("nome" to "Teste", "email" to email, "senha" to "senha12345"),
                status = 201,
            )
        val sessao =
            chamar("POST", "/sessoes", dados = mapOf("email" to email, "senha" to "senha12345"))
        return Jogador(sessao["token"].asString(), u["id"].asLong(), email)
    }

    private fun personagem(j: Jogador, nome: String = "Falken") =
        chamar("POST", "/personagens", j.token, mapOf("nome" to nome), 201)["id"].asLong()

    private fun campanha(j: Jogador): Long {
        val personagemId = personagem(j)
        return chamar(
                "POST",
                "/campanhas",
                j.token,
                mapOf("titulo" to "Aventura", "personagemId" to personagemId, "livroId" to 1),
                201,
            )["id"]
            .asLong()
    }

    private fun configurar(j: Jogador) =
        chamar(
            "PATCH",
            "/configuracao",
            j.token,
            mapOf("provedor" to "OpenAI", "modelo" to "modelo-teste", "chave" to "segredo-api-1234"),
        )

    private fun acao(
        j: Jogador,
        campanhaId: Long,
        texto: String = "Investigo o cais",
        uuid: String = UUID(0, sequencia.getAndIncrement()).toString(),
        status: Int = 201,
    ) =
        chamar(
            "POST",
            "/campanhas/$campanhaId/mensagens",
            j.token,
            mapOf("texto" to texto, "requisicaoId" to uuid),
            status,
        )

    private fun gerarNarracao(
        jogador: Jogador,
        campanhaId: Long,
        statusEsperado: Int = 200,
    ): JsonNode {
        val inicio =
            simuladorHttp
                .perform(
                    MockMvcRequestBuilders.post("/api/campanhas/$campanhaId/narracoes")
                        .contextPath("/api")
                        .header("Authorization", "Bearer ${jogador.token}")
                        .accept(MediaType.TEXT_EVENT_STREAM)
                )
                .andExpect(MockMvcResultMatchers.request().asyncStarted())
                .andReturn()
        inicio.getAsyncResult(10000)
        val resultado =
            simuladorHttp.perform(MockMvcRequestBuilders.asyncDispatch(inicio)).andReturn()
        val dados =
            resultado.response.contentAsString
                .lineSequence()
                .filter { it.startsWith("data:") }
                .map { conversorJson.readTree(it.substringAfter("data:")) }
                .last()
        assertEquals(statusEsperado, if (dados.has("status")) dados["status"].asInt() else 200)
        return dados
    }

    @Test
    fun `cadastro valida dados e impede emails duplicados sem expor senha`() {
        val j = conta()
        val perfil = chamar("GET", "/perfil", j.token)
        assertFalse(perfil.has("senhaHash"))
        chamar(
            "POST",
            "/usuarios",
            dados =
                mapOf("nome" to "Outro", "email" to j.email.uppercase(), "senha" to "senha12345"),
            status = 409,
        )
        chamar(
            "POST",
            "/usuarios",
            dados = mapOf("nome" to " ", "email" to "invalido", "senha" to "123"),
            status = 400,
        )
        assertNotEquals("senha12345", usuarios.findById(j.id).get().senhaHash)
    }

    @Test
    fun `login incorreto e token adulterado sao rejeitados`() {
        val j = conta()
        val erro =
            chamar(
                "POST",
                "/sessoes",
                dados = mapOf("email" to j.email, "senha" to "errada"),
                status = 401,
            )
        assertEquals("E-mail ou senha incorretos.", erro["detail"].asString())
        chamar("GET", "/perfil", j.token + "x", status = 401)
        chamar("GET", "/perfil", status = 401)
        chamar("DELETE", "/personagens/1", status = 401)
    }

    @Test
    fun `logout revoga o token no servidor`() {
        val j = conta()
        chamar("DELETE", "/sessoes", j.token, status = 204)
        chamar("GET", "/perfil", j.token, status = 401)
        chamar("POST", "/sessoes", dados = mapOf("email" to j.email, "senha" to "senha12345"))
    }

    @Test
    fun `perfil pode mudar e troca de senha exige senha atual e encerra sessoes`() {
        val j = conta()
        val outro = conta()
        chamar("PUT", "/perfil", j.token, mapOf("nome" to "Atualizado", "email" to j.email))
        assertEquals("Atualizado", chamar("GET", "/perfil", j.token)["nome"].asString())
        chamar("PUT", "/perfil", j.token, mapOf("nome" to "Teste", "email" to outro.email), 409)
        chamar(
            "PUT",
            "/perfil/senha",
            j.token,
            mapOf("senhaAtual" to "errada", "novaSenha" to "senhaNova123"),
            400,
        )
        chamar(
            "PUT",
            "/perfil/senha",
            j.token,
            mapOf("senhaAtual" to "senha12345", "novaSenha" to "senhaNova123"),
            204,
        )
        chamar("GET", "/perfil", j.token, status = 401)
        chamar("POST", "/sessoes", dados = mapOf("email" to j.email, "senha" to "senhaNova123"))
    }

    @Test
    fun `CRUD de personagem isola os dados por usuario`() {
        val j = conta()
        val outro = conta()
        val id = personagem(j)
        assertEquals(0, chamar("GET", "/personagens", outro.token)["total"].asInt())
        chamar("GET", "/personagens/$id", outro.token, status = 404)
        chamar("PUT", "/personagens/$id", outro.token, mapOf("nome" to "Invasor"), 404)
        chamar("DELETE", "/personagens/$id", outro.token, status = 404)
        chamar("PUT", "/personagens/$id", j.token, mapOf("nome" to "Aurel"))
        assertEquals("Aurel", chamar("GET", "/personagens/$id", j.token)["nome"].asString())
        chamar("DELETE", "/personagens/$id", j.token, status = 204)
        chamar("GET", "/personagens/$id", j.token, status = 404)
    }

    @Test
    fun `consulta permite busca ordenacao e paginacao validadas`() {
        val j = conta()
        personagem(j, "Zoe")
        personagem(j, "Ana")
        val r = chamar("GET", "/personagens?ordenarPor=nome&direcao=asc&tamanho=1", j.token)
        assertEquals("Ana", r["conteudo"][0]["nome"].asString())
        assertFalse(r["ultima"].asBoolean())
        assertEquals(1, chamar("GET", "/personagens?busca=Zo", j.token)["total"].asInt())
        chamar("GET", "/personagens?tamanho=101", j.token, status = 400)
        chamar("GET", "/personagens?ordenarPor=senhaHash", j.token, status = 400)
    }

    @Test
    fun `catalogo filtra titulo e genero`() {
        val j = conta()
        val livros = chamar("GET", "/livros?busca=Farol&genero=Fantasia", j.token)
        assertEquals(1, livros["total"].asInt())
        assertEquals(1L, livros["conteudo"][0]["id"].asLong())
    }

    @Test
    fun `campanha so associa personagem do proprio usuario`() {
        val j = conta()
        val outro = conta()
        val personagemId = personagem(outro)
        chamar(
            "POST",
            "/campanhas",
            j.token,
            mapOf("titulo" to "Falha", "personagemId" to personagemId, "livroId" to 1),
            404,
        )
        assertEquals(0, chamar("GET", "/campanhas", j.token)["total"].asInt())
        val campanhaId = campanha(j)
        assertEquals(
            "mestre",
            chamar("GET", "/campanhas/$campanhaId/mensagens", j.token)["mensagens"][0]["autor"]
                .asString(),
        )
        chamar("GET", "/campanhas/$campanhaId/mensagens", outro.token, status = 404)
    }

    @Test
    fun `exclusao protege personagem vinculado e remove filhos da campanha`() {
        val j = conta()
        val campanhaId = campanha(j)
        val personagemId =
            chamar("GET", "/campanhas", j.token)["conteudo"]
                .first { it["id"].asLong() == campanhaId }["personagemId"]
                .asLong()
        chamar(
            "POST",
            "/campanhas/$campanhaId/memorias",
            j.token,
            mapOf("titulo" to "Fato", "texto" to "Lia ajuda"),
            201,
        )
        chamar("DELETE", "/personagens/$personagemId", j.token, status = 409)
        chamar("DELETE", "/campanhas/$campanhaId", j.token, status = 204)
        assertEquals(0L, mensagens.contarDaCampanha(campanhaId))
        assertEquals(0, memorias.listarAtivas(campanhaId).size)
        chamar("DELETE", "/personagens/$personagemId", j.token, status = 204)
    }

    @Test
    fun `CRUD de memoria valida campanha e alterna atividade`() {
        val j = conta()
        val outro = conta()
        val campanhaId = campanha(j)
        val cid2 = campanha(j)
        val memoriaId =
            chamar(
                    "POST",
                    "/campanhas/$campanhaId/memorias",
                    j.token,
                    mapOf("titulo" to "Fato", "texto" to "Lia ajuda"),
                    201,
                )["id"]
                .asLong()
        chamar("GET", "/campanhas/$campanhaId/memorias/$memoriaId", outro.token, status = 404)
        chamar(
            "PUT",
            "/campanhas/$cid2/memorias/$memoriaId",
            j.token,
            mapOf("titulo" to "Roubo", "texto" to "Teste"),
            404,
        )
        chamar(
            "PUT",
            "/campanhas/$campanhaId/memorias/$memoriaId",
            j.token,
            mapOf("titulo" to "Lia", "texto" to "Lia é cartógrafa"),
        )
        chamar(
            "PUT",
            "/campanhas/$campanhaId/memorias/$memoriaId",
            j.token,
            mapOf("titulo" to "Lia", "texto" to "Lia é cartógrafa", "ativa" to false),
        )
        assertEquals(
            0,
            chamar("GET", "/campanhas/$campanhaId/memorias?ativa=true", j.token)["total"].asInt(),
        )
        assertFalse(
            chamar("GET", "/campanhas/$campanhaId/memorias/$memoriaId", j.token)["ativa"]
                .asBoolean()
        )
        chamar("DELETE", "/campanhas/$campanhaId/memorias/$memoriaId", j.token, status = 204)
        chamar("GET", "/campanhas/$campanhaId/memorias/$memoriaId", j.token, status = 404)
    }

    @Test
    fun `acao usa idempotencia e impede duas acoes consecutivas sem resposta`() {
        val j = conta()
        val campanhaId = campanha(j)
        val uuid = UUID(0, sequencia.getAndIncrement()).toString()
        val a = acao(j, campanhaId, uuid = uuid)
        val b = acao(j, campanhaId, uuid = uuid)
        assertEquals(a["id"], b["id"])
        acao(j, campanhaId, "Outra ação", uuid, 409)
        acao(j, campanhaId, status = 409)
        assertEquals(2L, mensagens.contarDaCampanha(campanhaId))
    }

    @Test
    fun `falha da IA preserva acao e permite retentar sem duplicacao`() {
        val j = conta()
        val campanhaId = campanha(j)
        configurar(j)
        acao(j, campanhaId)
        provedorSimulado.falhar = true
        gerarNarracao(j, campanhaId, 502)
        assertEquals(
            "jogador",
            chamar("GET", "/campanhas/$campanhaId/mensagens", j.token)["mensagens"]
                .last()["autor"]
                .asString(),
        )
        provedorSimulado.falhar = false
        val r = gerarNarracao(j, campanhaId)
        val repetida = gerarNarracao(j, campanhaId)
        assertEquals(r["id"], repetida["id"])
        assertEquals(2, provedorSimulado.chamadas)
        assertEquals(3L, mensagens.contarDaCampanha(campanhaId))
    }

    @Test
    fun `contexto respeita visibilidade e memorias ativas`() {
        val j = conta()
        val campanhaId = campanha(j)
        configurar(j)
        val personagemId =
            chamar("GET", "/campanhas", j.token)["conteudo"]
                .first { it["id"].asLong() == campanhaId }["personagemId"]
                .asLong()
        chamar(
            "PUT",
            "/personagens/$personagemId",
            j.token,
            mapOf(
                "nome" to "Falken",
                "segredos" to "SEGREDO_OCULTO_9287",
                "compartilharSegredos" to false,
            ),
        )
        chamar(
            "POST",
            "/campanhas/$campanhaId/memorias",
            j.token,
            mapOf("titulo" to "Lembrar", "texto" to "MEMORIA_ATIVA_337"),
            201,
        )
        chamar(
            "POST",
            "/campanhas/$campanhaId/memorias",
            j.token,
            mapOf("titulo" to "Esquecer", "texto" to "MEMORIA_INATIVA_338", "ativa" to false),
            201,
        )
        acao(j, campanhaId)
        gerarNarracao(j, campanhaId)
        val sistema = provedorSimulado.ultimo!!.mensagens.first().conteudo
        assertTrue(sistema.contains("Falken"))
        assertTrue(sistema.contains("MEMORIA_ATIVA_337"))
        assertFalse(sistema.contains("SEGREDO_OCULTO_9287"))
        assertFalse(sistema.contains("MEMORIA_INATIVA_338"))
        assertEquals("user", provedorSimulado.ultimo!!.mensagens.last().papel)
    }

    @Test
    fun `chave e cifrada no banco e nunca retornada pela API`() {
        val j = conta()
        val r = configurar(j)
        assertEquals("1234", r["finalChave"].asString())
        assertFalse(r.has("chave"))
        assertFalse(r.has("chaveCifrada"))
        val salvo = configuracoes.buscarPorUsuario(j.id)!!
        assertNotEquals("segredo-api-1234", salvo.chaveCifrada)
        assertEquals("segredo-api-1234", cifra.decifrar(salvo.chaveCifrada!!))
        chamar(
            "PATCH",
            "/configuracao",
            j.token,
            mapOf("provedor" to "OpenAI", "modelo" to "teste", "chave" to "invalida"),
            400,
        )
        assertEquals("1234", chamar("GET", "/configuracao", j.token)["finalChave"].asString())
        chamar("PATCH", "/configuracao", j.token, mapOf("removerChave" to true))
        assertFalse(chamar("GET", "/configuracao", j.token)["chaveConfigurada"].asBoolean())
    }

    @Test
    fun `preferencias invalidas nao substituem as anteriores`() {
        val j = conta()
        chamar("PATCH", "/configuracao", j.token, mapOf("tom" to "Sombrio", "criatividade" to 0.4))
        chamar("PATCH", "/configuracao", j.token, mapOf("criatividade" to 2), 400)
        assertEquals(0.4, chamar("GET", "/configuracao", j.token)["criatividade"].asDouble())
    }

    @Test
    fun `historico paginado nao perde nem repete mensagens`() {
        val j = conta()
        val campanhaId = campanha(j)
        val c = campanhas.findById(campanhaId).get()
        mensagens.saveAll(
            (1..35).map { Mensagem(campanha = c, autor = "mestre", texto = "Mensagem $it") }
        )
        val recentes = chamar("GET", "/campanhas/$campanhaId/mensagens?limite=30", j.token)
        assertEquals(36, recentes["total"].asInt())
        assertTrue(recentes["temAnteriores"].asBoolean())
        val primeira = recentes["mensagens"].first()["id"].asLong()
        val antigas =
            chamar("GET", "/campanhas/$campanhaId/mensagens?antesDe=$primeira&limite=30", j.token)
        assertEquals(6, antigas["mensagens"].size())
        assertTrue(antigas["mensagens"].all { it["id"].asLong() < primeira })
    }

    @Test
    fun `SSE entrega trechos e confirma mensagem salva`() {
        val j = conta()
        val campanhaId = campanha(j)
        configurar(j)
        acao(j, campanhaId)
        val inicial =
            simuladorHttp
                .perform(
                    MockMvcRequestBuilders.post("/api/campanhas/$campanhaId/narracoes")
                        .contextPath("/api")
                        .header("Authorization", "Bearer ${j.token}")
                        .accept(MediaType.TEXT_EVENT_STREAM)
                )
                .andExpect(MockMvcResultMatchers.request().asyncStarted())
                .andReturn()
        inicial.getAsyncResult(10000)
        val fim =
            simuladorHttp
                .perform(MockMvcRequestBuilders.asyncDispatch(inicial))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        assertTrue(fim.response.contentAsString.contains("event:trecho"))
        assertTrue(fim.response.contentAsString.contains("event:concluida"))
        assertEquals(3L, mensagens.contarDaCampanha(campanhaId))
    }

    @Test
    fun `campos ausentes geram ProblemDetail com todos os erros`() {
        val erro = chamar("POST", "/usuarios", dados = emptyMap<String, String>(), status = 400)
        assertEquals("Bad Request", erro["title"].asString())
        assertEquals("/api/usuarios", erro["instance"].asString())
        listOf("nome", "email", "senha").forEach { assertTrue(erro["errors"].has(it)) }
        assertFalse(erro.has("exception"))
        assertFalse(erro.has("trace"))
    }

    @Test
    fun `JSON malformado informa o erro de corpo sem expor detalhes internos`() {
        val resposta =
            simuladorHttp
                .perform(
                    MockMvcRequestBuilders.post("/api/usuarios")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{")
                )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()
                .response
        assertTrue(conversorJson.readTree(resposta.contentAsString)["errors"].has("body"))
    }

    @Test
    fun `parametros de rota invalidos e rota desconhecida preservam os status HTTP`() {
        val jogador = conta()
        chamar("GET", "/personagens/abc", jogador.token, status = 400)
        chamar("GET", "/personagens/-1", jogador.token, status = 400)
        chamar("GET", "/livros/1", jogador.token, status = 404)
        chamar("GET", "/rota-inexistente", jogador.token, status = 404)
        chamar("PUT", "/livros", jogador.token, mapOf("titulo" to "Teste"), 405)
    }

    @Test
    fun `PATCH de preferencias mantem o provedor e a chave existentes`() {
        val jogador = conta()
        configurar(jogador)
        chamar("PATCH", "/configuracao", jogador.token, mapOf("tom" to "Sombrio"))
        val configuracao = chamar("GET", "/configuracao", jogador.token)
        assertEquals("OpenAI", configuracao["provedor"].asString())
        assertTrue(configuracao["chaveConfigurada"].asBoolean())
        assertEquals("Sombrio", configuracao["tom"].asString())
    }

    @Test
    fun `login com senha multibyte excessiva retorna erro de credenciais`() {
        val jogador = conta()
        chamar(
            "POST",
            "/sessoes",
            dados = mapOf("email" to jogador.email, "senha" to "á".repeat(40)),
            status = 401,
        )
    }

    @Test
    fun `preflight CORS aceita somente origens configuradas`() {
        simuladorHttp
            .perform(
                MockMvcRequestBuilders.options("/api/personagens")
                    .contextPath("/api")
                    .header("Origin", "http://localhost:5173")
                    .header("Access-Control-Request-Method", "POST")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.header()
                    .string("Access-Control-Allow-Origin", "http://localhost:5173")
            )
        simuladorHttp
            .perform(
                MockMvcRequestBuilders.options("/api/personagens")
                    .contextPath("/api")
                    .header("Origin", "https://outra-origem.exemplo.com")
                    .header("Access-Control-Request-Method", "POST")
            )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
    }
}
