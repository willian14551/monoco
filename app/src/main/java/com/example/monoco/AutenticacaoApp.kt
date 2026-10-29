package com.example.monoco

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.compose.*
import com.example.monoco.dados.RepositorioMonoco
import com.example.monoco.dados.local.BancoMonoco
import com.example.monoco.dados.remoto.*
import com.example.monoco.ui.telas.boasvindas.TelaBoasVindas
import com.example.monoco.ui.telas.cadastro.TelaCadastro
import com.example.monoco.ui.telas.login.TelaLogin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URI

@Composable
fun MonocoApp(modifier: Modifier=Modifier) {
    val contexto=LocalContext.current.applicationContext
    val cofre=remember { SessaoSegura(contexto) }
    var sessao by remember { mutableStateOf(cofre.ler()) }
    var ocupado by remember { mutableStateOf(false) }
    var configurarEndereco by remember { mutableStateOf(false) }
    var endereco by remember { mutableStateOf(cofre.endereco) }
    var erroEndereco by remember { mutableStateOf("") }
    val escopo=rememberCoroutineScope()
    val avisos=remember { SnackbarHostState() }
    val api=remember { ApiMonoco(cofre) { escopo.launch { sessao=null } } }
    fun executar(acao: suspend ()->Unit) {
        if(ocupado) return
        ocupado=true
        escopo.launch {
            try { acao() }
            catch(e: Exception) { if(e is kotlinx.coroutines.CancellationException) throw e; avisos.showSnackbar(e.message ?: "Não foi possível concluir.") }
            finally { ocupado=false }
        }
    }
    LaunchedEffect(sessao?.expiraEm) {
        sessao?.let {
            delay((it.expiraEm-System.currentTimeMillis()).coerceAtLeast(0))
            cofre.limpar(); sessao=null; avisos.showSnackbar("Sessão expirada. Entre novamente.")
        }
    }
    val conta=sessao
    if(conta!=null) {
        key(conta.usuario.id,conta.expiraEm) {
            val banco=remember { BancoMonoco.paraConta(contexto,conta.usuario.id,cofre.endereco) }
            val repositorio=remember { RepositorioMonoco(banco,api) }
            val dono=remember { object: ViewModelStoreOwner { override val viewModelStore=ViewModelStore() } }
            DisposableEffect(Unit) { onDispose { dono.viewModelStore.clear(); banco.close() } }
            CompositionLocalProvider(LocalViewModelStoreOwner provides dono) {
                AplicativoConectado(repositorio,aoSair={ executar {
                    try { api.sair() } finally { sessao=null }
                } },modifier=modifier)
            }
        }
    } else {
        val navegador=rememberNavController()
        Scaffold(modifier=modifier.fillMaxSize(),snackbarHost={ SnackbarHost(avisos) },topBar={
            Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),horizontalArrangement=Arrangement.SpaceBetween,
                verticalAlignment=Alignment.CenterVertically) {
                Text("Monoco",style=MaterialTheme.typography.titleMedium)
                TextButton(onClick={ configurarEndereco=true },enabled=!ocupado) { Text("Servidor") }
            }
        }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                NavHost(navegador,startDestination="boas_vindas") {
                    composable("boas_vindas") { TelaBoasVindas(
                        aoComecarAventura={ navegador.navigate("cadastro") },aoEntrar={ navegador.navigate("login") }) }
                    composable("cadastro") { TelaCadastro(aoVoltar={ navegador.popBackStack() },aoCadastrar={ nome,email,senha ->
                        executar {
                            api.cadastrar(nome,email,senha)
                            navegador.navigate("login") { popUpTo("boas_vindas") }
                            escopo.launch { avisos.showSnackbar("Conta criada com sucesso. Entre para continuar.") }
                        }
                    }) }
                    composable("login") { TelaLogin(aoVoltar={ navegador.popBackStack() },
                        aoCadastrar={ navegador.navigate("cadastro") },aoEntrar={ email,senha ->
                            executar { sessao=api.entrar(email,senha) }
                        }) }
                }
                if(ocupado) LinearProgressIndicator(Modifier.align(Alignment.TopCenter).fillMaxWidth())
            }
        }
    }
    if(configurarEndereco) AlertDialog(onDismissRequest={ configurarEndereco=false },title={ Text("Conectar ao backend") },
        text={ Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Emulador: http://10.0.2.2:8080/api/\nCelular: use o IP do computador na mesma rede.")
            OutlinedTextField(value=endereco,onValueChange={ endereco=it; erroEndereco="" },label={ Text("URL da API") },isError=erroEndereco.isNotEmpty(),singleLine=true)
            if(erroEndereco.isNotEmpty()) Text(erroEndereco,color=MaterialTheme.colorScheme.error)
        } },confirmButton={ TextButton(onClick={
            val uri=runCatching { URI(endereco.trim()) }.getOrNull()
            if(uri?.host==null || uri.scheme !in listOf("https","http") || uri.userInfo!=null || uri.query!=null || uri.fragment!=null) erroEndereco="Informe uma URL válida, terminando em /api/."
            else if(!BuildConfig.DEBUG && uri.scheme!="https") erroEndereco="Use HTTPS na versão de produção."
            else { cofre.endereco=endereco; configurarEndereco=false }
        }) { Text("Salvar endereço") } },dismissButton={ TextButton(onClick={ configurarEndereco=false }) { Text("Cancelar") } })
}
