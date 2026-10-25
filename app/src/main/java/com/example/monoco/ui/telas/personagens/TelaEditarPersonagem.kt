package com.example.monoco.ui.telas.personagens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.monoco.dados.local.Personagem
import com.example.monoco.ui.componentes.*

@Composable
fun TelaEditarPersonagem(personagem: Personagem?, salvando: Boolean, aoVoltar: () -> Unit,
    aoSalvar: (Personagem) -> Unit) {
    var nome by rememberSaveable { mutableStateOf(personagem?.nome.orEmpty()) }
    var classe by rememberSaveable { mutableStateOf(personagem?.classe.orEmpty()) }
    var atributos by rememberSaveable { mutableStateOf(personagem?.atributos.orEmpty()) }
    var historia by rememberSaveable { mutableStateOf(personagem?.historia.orEmpty()) }
    var segredos by rememberSaveable { mutableStateOf(personagem?.segredos.orEmpty()) }
    var compartilharNome by rememberSaveable { mutableStateOf(personagem?.compartilharNome ?: true) }
    var compartilharClasse by rememberSaveable { mutableStateOf(personagem?.compartilharClasse ?: true) }
    var compartilharAtributos by rememberSaveable { mutableStateOf(personagem?.compartilharAtributos ?: true) }
    var compartilharHistoria by rememberSaveable { mutableStateOf(personagem?.compartilharHistoria ?: true) }
    var compartilharSegredos by rememberSaveable { mutableStateOf(personagem?.compartilharSegredos ?: true) }
    var tentouSalvar by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CabecalhoTela(if (personagem == null) "Novo personagem" else "Editar personagem",
            "A ficha é livre: use os atributos que fizerem sentido para a sua história.", aoVoltar)
        AvisoLocal("Você escolhe o que o mestre pode saber. Campos desmarcados ficam fora do contexto do narrador.")
        CampoFicha("Nome *", nome, { nome = it }, compartilharNome, { compartilharNome = it },
            erro = tentouSalvar && nome.isBlank())
        CampoFicha("Classe ou papel", classe, { classe = it }, compartilharClasse, { compartilharClasse = it })
        CampoFicha("Atributos", atributos, { atributos = it }, compartilharAtributos, { compartilharAtributos = it },
            variasLinhas = true, ajuda = "Ex.: Força 12, Carisma 16, Investigação +3.")
        CampoFicha("História", historia, { historia = it }, compartilharHistoria, { compartilharHistoria = it }, variasLinhas = true)
        CampoFicha("Segredos", segredos, { segredos = it }, compartilharSegredos, { compartilharSegredos = it }, variasLinhas = true)
        BotaoSalvar("Salvar personagem", salvando = salvando) {
            tentouSalvar = true
            if (nome.isNotBlank()) aoSalvar(Personagem(
                id = personagem?.id ?: 0,
                nome = nome.trim(), classe = classe.trim(), atributos = atributos.trim(),
                historia = historia.trim(), segredos = segredos.trim(),
                compartilharNome = compartilharNome, compartilharClasse = compartilharClasse,
                compartilharAtributos = compartilharAtributos, compartilharHistoria = compartilharHistoria,
                compartilharSegredos = compartilharSegredos))
        }
    }
}

@Composable
private fun CampoFicha(titulo: String, valor: String, aoAlterar: (String) -> Unit,
    visivel: Boolean, aoMudarVisibilidade: (Boolean) -> Unit, variasLinhas: Boolean = false,
    erro: Boolean = false, ajuda: String? = null) {
    Column {
        OutlinedTextField(value = valor, onValueChange = aoAlterar, label = { Text(titulo) },
            singleLine = !variasLinhas, minLines = if (variasLinhas) 3 else 1,
            isError = erro, supportingText = {
                if (erro) Text("O nome do personagem é obrigatório.") else if (ajuda != null) Text(ajuda)
            }, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { aoMudarVisibilidade(!visivel) }) {
                Text(if (visivel) "Disponível para o mestre" else "Oculto do mestre")
            }
            Switch(checked = visivel, onCheckedChange = aoMudarVisibilidade,
                modifier = Modifier.semantics { contentDescription = "Disponibilizar $titulo para o mestre" })
        }
    }
}
