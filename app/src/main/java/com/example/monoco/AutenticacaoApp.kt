package com.example.monoco

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.compose.*
import com.example.monoco.dados.remoto.ApiMonoco
import com.example.monoco.dados.remoto.ConfiguracaoServidor
import com.example.monoco.dados.remoto.SessaoSegura
import com.example.monoco.ui.telas.boasvindas.TelaBoasVindas
import com.example.monoco.ui.telas.cadastro.TelaCadastro
import com.example.monoco.ui.telas.login.TelaLogin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MonocoApp(cofre: SessaoSegura, api: ApiMonoco, modifier: Modifier = Modifier) {
    val sessao by cofre.sessao.collectAsStateWithLifecycle()
    val donoPrincipal = checkNotNull(LocalViewModelStoreOwner.current) as HasDefaultViewModelProviderFactory
    var ocupado by remember { mutableStateOf(false) }
    var configurarEndereco by remember { mutableStateOf(false) }
    var enderecoEmUso by remember { mutableStateOf(cofre.endereco) }
    var endereco by remember { mutableStateOf(enderecoEmUso) }
    var erroEndereco by remember { mutableStateOf("") }
    var servidorDisponivel by remember { mutableStateOf<Boolean?>(null) }
    var mensagemConexao by remember { mutableStateOf("") }
    val escopo = rememberCoroutineScope()
    val avisos = remember { SnackbarHostState() }

    fun executar(acao: suspend () -> Unit) {
        if (ocupado) return
        ocupado = true
        escopo.launch {
            try {
                acao()
            } catch (erro: Exception) {
                if (erro is CancellationException) throw erro
                avisos.showSnackbar(erro.message ?: "Não foi possível concluir.")
            } finally {
                ocupado = false
            }
        }
    }

    suspend fun verificarServidor() {
        servidorDisponivel = null
        mensagemConexao = ""
        try {
            api.verificarConexao(enderecoEmUso)
            servidorDisponivel = true
        } catch (erro: Exception) {
            if (erro is CancellationException) throw erro
            servidorDisponivel = false
            mensagemConexao = erro.message ?: "Não foi possível acessar o servidor."
        }
    }

    LaunchedEffect(enderecoEmUso) { verificarServidor() }
    LaunchedEffect(sessao?.expiraEm) {
        sessao?.let { conta ->
            delay((conta.expiraEm - System.currentTimeMillis()).coerceAtLeast(0))
            cofre.limpar()
        }
    }

    val conta = sessao
    if (conta != null) {
        key(conta.usuario.id, conta.expiraEm) {
            val dono = remember(donoPrincipal) {
                object : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
                    override val viewModelStore = ViewModelStore()
                    override val defaultViewModelProviderFactory = donoPrincipal.defaultViewModelProviderFactory
                    override val defaultViewModelCreationExtras = donoPrincipal.defaultViewModelCreationExtras
                }
            }
            DisposableEffect(dono) { onDispose { dono.viewModelStore.clear() } }
            CompositionLocalProvider(LocalViewModelStoreOwner provides dono) {
                AplicativoConectado(aoSair = { executar { api.sair() } }, modifier = modifier)
            }
        }
    } else {
        val navegador = rememberNavController()
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(avisos) },
            topBar = {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Monoco", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = {
                        endereco = enderecoEmUso
                        erroEndereco = ""
                        configurarEndereco = true
                    }, enabled = !ocupado) {
                        Text(when (servidorDisponivel) {
                            true -> "Servidor · conectado"
                            false -> "Servidor · sem conexão"
                            null -> "Servidor · verificando"
                        })
                    }
                }
            }
        ) { espacamento ->
            Box(Modifier.fillMaxSize().padding(espacamento)) {
                NavHost(navegador, startDestination = "boas_vindas") {
                    composable("boas_vindas") {
                        TelaBoasVindas(
                            aoComecarAventura = { navegador.navigate("cadastro") },
                            aoEntrar = { navegador.navigate("login") }
                        )
                    }
                    composable("cadastro") {
                        TelaCadastro(aoVoltar = { navegador.popBackStack() }, aoCadastrar = { nome, email, senha ->
                            executar {
                                api.cadastrar(nome, email, senha)
                                navegador.navigate("login") { popUpTo("boas_vindas") }
                                escopo.launch { avisos.showSnackbar("Conta criada. Entre para continuar.") }
                            }
                        })
                    }
                    composable("login") {
                        TelaLogin(
                            aoVoltar = { navegador.popBackStack() },
                            aoCadastrar = { navegador.navigate("cadastro") },
                            aoEntrar = { email, senha -> executar { api.entrar(email, senha) } }
                        )
                    }
                }
                if (ocupado) LinearProgressIndicator(Modifier.align(Alignment.TopCenter).fillMaxWidth())
            }
        }
    }

    if (configurarEndereco) AlertDialog(
        onDismissRequest = { configurarEndereco = false },
        title = { Text("Servidor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("O endereço padrão conecta o emulador ao Docker na porta 8081. No celular, use o IP do computador na mesma rede.")
                OutlinedTextField(
                    value = endereco,
                    onValueChange = { endereco = it; erroEndereco = "" },
                    label = { Text("Endereço da API") },
                    isError = erroEndereco.isNotEmpty(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = { endereco = ConfiguracaoServidor.ENDERECO_PADRAO; erroEndereco = "" }) {
                    Text("Usar endereço padrão")
                }
                if (erroEndereco.isNotEmpty()) Text(erroEndereco, color = MaterialTheme.colorScheme.error)
                else if (mensagemConexao.isNotEmpty()) Text(mensagemConexao, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { escopo.launch { verificarServidor() } }, enabled = servidorDisponivel != null) {
                    Text("Verificar conexão atual")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                try {
                    val normalizado = ConfiguracaoServidor.normalizarEndereco(endereco, permitirHttp = BuildConfig.DEBUG)
                    cofre.endereco = normalizado
                    if (enderecoEmUso == normalizado) escopo.launch { verificarServidor() }
                    else enderecoEmUso = normalizado
                    configurarEndereco = false
                } catch (erro: IllegalArgumentException) {
                    erroEndereco = erro.message.orEmpty()
                }
            }) { Text("Salvar e verificar") }
        },
        dismissButton = { TextButton(onClick = { configurarEndereco = false }) { Text("Cancelar") } }
    )
}
