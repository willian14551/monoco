package com.example.monoco

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.monoco.dados.RepositorioMonoco
import com.example.monoco.dados.local.BancoMonoco
import com.example.monoco.ui.MonocoViewModel
import com.example.monoco.ui.componentes.EstadoVazio
import com.example.monoco.ui.telas.boasvindas.TelaBoasVindas
import com.example.monoco.ui.telas.cadastro.TelaCadastro
import com.example.monoco.ui.telas.campanhas.*
import com.example.monoco.ui.telas.chat.TelaChat
import com.example.monoco.ui.telas.configuracao.TelaConfiguracao
import com.example.monoco.ui.telas.livros.*
import com.example.monoco.ui.telas.login.TelaLogin
import com.example.monoco.ui.telas.memorias.*
import com.example.monoco.ui.telas.perfil.TelaPerfil
import com.example.monoco.ui.telas.personagens.*

private object Rotas {
    const val BOAS_VINDAS = "boas_vindas"
    const val CADASTRO = "cadastro"
    const val LOGIN = "login"
    const val CAMPANHAS = "campanhas"
    const val PERSONAGENS = "personagens"
    const val LIVROS = "livros"
    const val PERFIL = "perfil"
    const val CONFIGURACAO = "configuracao"
    const val PERSONAGEM = "personagem/{id}"
    const val LIVRO = "livro/{id}"
    const val NOVA_CAMPANHA = "nova_campanha?livroId={livroId}"
    const val CHAT = "campanha/{id}"
    const val MEMORIAS = "memorias/{id}"
    const val MEMORIA = "memoria/{campanhaId}/{memoriaId}?mensagemId={mensagemId}"
}

private data class Aba(val rota: String, val titulo: String, val icone: Int)
private val abas = listOf(
    Aba(Rotas.CAMPANHAS, "Campanhas", R.drawable.icone_campanhas),
    Aba(Rotas.LIVROS, "Livros", R.drawable.icone_livros),
    Aba(Rotas.PERSONAGENS, "Personagens", R.drawable.icone_personagens),
    Aba(Rotas.PERFIL, "Perfil", R.drawable.icone_perfil)
)

@Composable
fun AplicativoConectado(repositorio: RepositorioMonoco, aoSair: () -> Unit, modifier: Modifier = Modifier) {
    val modelo: MonocoViewModel = viewModel(factory = MonocoViewModel.Fabrica(repositorio))
    val estado by modelo.estado.collectAsStateWithLifecycle()
    val config by modelo.configuracaoOnline.collectAsStateWithLifecycle()
    val email by modelo.emailOnline.collectAsStateWithLifecycle()
    val dados = estado.dados
    val controlador = rememberNavController()
    val entradaAtual by controlador.currentBackStackEntryAsState()
    val rotaAtual = entradaAtual?.destination?.route
    val avisos = remember { SnackbarHostState() }

    LaunchedEffect(modelo) {
        modelo.mensagensDeAviso.collect { avisos.showSnackbar(it) }
    }
    fun voltar() { controlador.popBackStack() }
    fun abrirMemoria(campanhaId: Long, memoriaId: Long = 0, mensagemId: Long = 0) {
        controlador.navigate("memoria/$campanhaId/$memoriaId?mensagemId=$mensagemId")
    }

    Scaffold(modifier = modifier.fillMaxSize(), snackbarHost = { SnackbarHost(avisos) },
        topBar = {
            Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),horizontalArrangement=Arrangement.SpaceBetween,
                verticalAlignment=Alignment.CenterVertically) {
                Text("Monoco · conta online",style=MaterialTheme.typography.labelMedium)
                TextButton(onClick=modelo::atualizarDados,enabled=!estado.operacao.salvando) { Text("Atualizar") }
            }
        },
        bottomBar = {
            if (abas.any { it.rota == rotaAtual }) NavigationBar {
                abas.forEach { aba ->
                    NavigationBarItem(selected = rotaAtual == aba.rota,
                        onClick = {
                            controlador.navigate(aba.rota) {
                                popUpTo(Rotas.CAMPANHAS) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }, icon = { Icon(painterResource(aba.icone), contentDescription = null) },
                        label = { Text(aba.titulo, maxLines = 1) })
                }
            }
        }) { espacamento ->
        if (estado.operacao.carregando) {
            Carregando(Modifier.padding(espacamento))
        } else NavHost(navController = controlador, startDestination = Rotas.CAMPANHAS,
            modifier = Modifier.fillMaxSize().padding(espacamento)) {
            composable(Rotas.CAMPANHAS) {
                TelaCampanhas(dados.perfil.nome, dados.campanhas, dados.personagens, dados.livros,
                    aoNovaCampanha = { controlador.navigate("nova_campanha?livroId=0") },
                    aoAbrirCampanha = { controlador.navigate("campanha/$it") },
                    aoExcluirCampanha = modelo::excluirCampanha)
            }
            composable(Rotas.PERSONAGENS) {
                TelaPersonagens(dados.personagens, dados.campanhas,
                    aoCriar = { controlador.navigate("personagem/0") },
                    aoEditar = { controlador.navigate("personagem/$it") }, aoExcluir = modelo::excluirPersonagem)
            }
            composable(Rotas.PERSONAGEM, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entrada ->
                val id = entrada.arguments?.getLong("id") ?: 0
                val personagem = dados.personagens.find { it.id == id }
                if (id != 0L && personagem == null) NaoEncontrado { voltar() }
                else TelaEditarPersonagem(personagem, estado.operacao.salvando, aoVoltar = { voltar() },
                    aoSalvar = { modelo.salvarPersonagem(it) { voltar() } })
            }
            composable(Rotas.LIVROS) {
                TelaLivros(dados.livros) { controlador.navigate("livro/$it") }
            }
            composable(Rotas.LIVRO, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entrada ->
                val livro = dados.livros.find { it.id == entrada.arguments?.getLong("id") }
                if (livro == null) NaoEncontrado { voltar() }
                else TelaDetalheLivro(livro, aoVoltar = { voltar() },
                    aoUsarLivro = { controlador.navigate("nova_campanha?livroId=${livro.id}") })
            }
            composable(Rotas.NOVA_CAMPANHA, arguments = listOf(navArgument("livroId") {
                type = NavType.LongType; defaultValue = 0L
            })) { entrada ->
                TelaNovaCampanha(dados.personagens, dados.livros, entrada.arguments?.getLong("livroId") ?: 0,
                    estado.operacao.salvando, aoVoltar = { voltar() },
                    aoCriarPersonagem = { controlador.navigate("personagem/0") },
                    aoIniciar = { titulo, personagemId, livroId ->
                        modelo.criarCampanha(titulo, personagemId, livroId) { id ->
                            controlador.navigate("campanha/$id") {
                                popUpTo(Rotas.NOVA_CAMPANHA) { inclusive = true }
                            }
                        }
                    })
            }
            composable(Rotas.CHAT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entrada ->
                val id = entrada.arguments?.getLong("id") ?: 0
                LaunchedEffect(id) { modelo.selecionarCampanha(id) }
                val campanha = dados.campanhas.find { it.id == id }
                val personagem = dados.personagens.find { it.id == campanha?.personagemId }
                val livro = dados.livros.find { it.id == campanha?.livroId }
                if (campanha == null || personagem == null || livro == null) NaoEncontrado { voltar() }
                else if (estado.campanha.id != id) Carregando()
                else TelaChat(campanha, personagem, livro, estado.campanha.mensagens,
                    estado.campanha.totalMensagens, estado.campanha.memorias.count { it.ativa },
                    narrando = estado.operacao.campanhaEmNarracao == id,
                    narracaoEmOutraCampanha = estado.operacao.campanhaEmNarracao?.let { it != id } ?: false,
                    trechoNarracao = estado.operacao.trechoNarracao,
                    aoVoltar = { voltar() }, aoMemorias = { controlador.navigate("memorias/$id") },
                    aoConfiguracoes = { controlador.navigate(Rotas.CONFIGURACAO) },
                    aoSalvarComoMemoria = { abrirMemoria(id, mensagemId = it) },
                    aoCarregarAnteriores = modelo::carregarMensagensAnteriores,
                    aoEnviar = { texto, aoSalvar -> modelo.enviarAcao(id, texto, aoSalvar) },
                    aoParar = modelo::pararNarracao,
                    aoTentarNovamente = { modelo.tentarNarracaoNovamente(id) },
                    rotuloNarrador = if(config.provedor=="Demonstracao") "Demonstração · sem IA" else "${config.provedor} · ${config.modelo}",
                    rotuloParar = "Parar de acompanhar")
            }
            composable(Rotas.MEMORIAS, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entrada ->
                val id = entrada.arguments?.getLong("id") ?: 0
                LaunchedEffect(id) { modelo.selecionarCampanha(id) }
                val campanha = dados.campanhas.find { it.id == id }
                if (campanha == null) NaoEncontrado { voltar() }
                else if (estado.campanha.id != id) Carregando()
                else TelaMemorias(campanha.titulo, estado.campanha.memorias, aoVoltar = { voltar() },
                    aoCriar = { abrirMemoria(id) }, aoEditar = { abrirMemoria(id, it) },
                    aoAlternar = modelo::alternarMemoria, aoExcluir = modelo::excluirMemoria)
            }
            composable(Rotas.MEMORIA, arguments = listOf(
                navArgument("campanhaId") { type = NavType.LongType },
                navArgument("memoriaId") { type = NavType.LongType },
                navArgument("mensagemId") { type = NavType.LongType; defaultValue = 0L }
            )) { entrada ->
                val campanhaId = entrada.arguments?.getLong("campanhaId") ?: 0
                val memoriaId = entrada.arguments?.getLong("memoriaId") ?: 0
                val mensagemId = entrada.arguments?.getLong("mensagemId") ?: 0
                LaunchedEffect(campanhaId) { modelo.selecionarCampanha(campanhaId) }
                val memoria = estado.campanha.memorias.find { it.id == memoriaId }
                val mensagem = estado.campanha.mensagens.find { it.id == mensagemId }
                if (dados.campanhas.none { it.id == campanhaId }) NaoEncontrado { voltar() }
                else if (estado.campanha.id != campanhaId) Carregando()
                else if (memoriaId != 0L && memoria == null) NaoEncontrado { voltar() }
                else TelaEditarMemoria(campanhaId, memoria, mensagem?.texto.orEmpty(), estado.operacao.salvando,
                    aoVoltar = { voltar() }, aoSalvar = { modelo.salvarMemoria(it) { voltar() } })
            }
            composable(Rotas.PERFIL) {
                com.example.monoco.ui.telas.perfil.TelaPerfilOnline(dados.perfil.nome,email,estado.operacao.salvando,
                    aoSalvar=modelo::salvarPerfilOnline,aoAlterarSenha=modelo::alterarSenhaOnline,
                    aoConfiguracoes={ controlador.navigate(Rotas.CONFIGURACAO) },aoSair={ modelo.pararNarracao(); aoSair() })
            }
            composable(Rotas.CONFIGURACAO) {
                TelaConfiguracao(dados.preferencias, estado.operacao.salvando, aoVoltar = { voltar() },
                    aoSalvar = modelo::salvarPreferencias,
                    aoTestarChave = {}, configuracaoOnline=config,
                    aoConfigurarProvedor=modelo::configurarProvedorOnline,aoRemoverChave=modelo::removerChaveOnline)
            }
        }
    }
}

@Composable
private fun Carregando(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun NaoEncontrado(aoVoltar: () -> Unit) {
    Box(Modifier.padding(24.dp)) {
        EstadoVazio("Conteúdo não encontrado", "Este item pode ter sido excluído.", "Voltar", aoVoltar)
    }
}
