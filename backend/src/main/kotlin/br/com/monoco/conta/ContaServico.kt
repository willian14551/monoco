package br.com.monoco.conta

import br.com.monoco.comum.*
import br.com.monoco.configuracao.*
import br.com.monoco.seguranca.*
import br.com.monoco.seguranca.Tokens
import java.util.Locale
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ContaServico(
    private val usuarios: UsuarioRepositorio,
    private val configuracoes: ConfiguracaoRepositorio,
    private val senhas: BCryptPasswordEncoder,
    private val tokens: Tokens,
    @param:Value("\${monoco.demo-permitida}") private val demonstracaoPermitida: Boolean,
) {
    private val registrador = LoggerFactory.getLogger(javaClass)
    private val hashInexistente = senhas.encode("credencial-inexistente-nao-autenticavel")

    private fun validarSenha(senha: String) {
        if (senha.length < 8 || senha.isBlank() || senha.toByteArray(Charsets.UTF_8).size > 72)
            invalido("A senha deve ter pelo menos 8 caracteres e no máximo 72 bytes em UTF-8.")
    }

    fun buscar(usuarioId: Long): Usuario =
        usuarios.findById(usuarioId).orElse(null) ?: naoEncontrado("Usuário")

    @Transactional
    fun cadastrar(dados: Cadastro): PerfilSaida {
        val senha = dados.senha.obrigatorio("Senha")
        validarSenha(senha)
        val nome = dados.nome.obrigatorio("Nome").trim()
        if (nome.length < 2) invalido("Informe um nome com pelo menos 2 caracteres.")
        val email = dados.email.obrigatorio("E-mail").trim().lowercase(Locale.ROOT)
        if (usuarios.existePorEmail(email)) conflito("E-mail já cadastrado.")
        val usuario =
            usuarios.save(
                Usuario(
                    nome = nome,
                    email = email,
                    senhaHash = senhas.encode(senha).obrigatorio("Hash da senha"),
                )
            )
        configuracoes.save(
            ConfiguracaoIa(
                usuario = usuario,
                provedor = if (demonstracaoPermitida) "Demonstracao" else "OpenAI",
            )
        )
        registrador.info("Conta criada: usuarioId={}", usuario.id)
        return usuario.paraResposta()
    }

    fun entrar(dados: Login): Sessao {
        val senha = dados.senha.orEmpty()
        val usuario = usuarios.buscarPorEmail(dados.email.orEmpty().trim().lowercase(Locale.ROOT))
        val senhaCabeNoBCrypt = senha.toByteArray(Charsets.UTF_8).size <= 72
        val senhaCorreta =
            senhaCabeNoBCrypt && senhas.matches(senha, usuario?.senhaHash ?: hashInexistente)
        if (usuario == null || !senhaCorreta)
            throw ErroNegocio(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos.")
        return tokens.emitir(usuario)
    }

    @Transactional
    fun editar(usuarioId: Long, dados: PerfilEntrada): PerfilSaida {
        val nome = dados.nome.obrigatorio("Nome").trim()
        if (nome.length < 2) invalido("Informe um nome com pelo menos 2 caracteres.")
        val usuario = buscar(usuarioId)
        val email = dados.email.obrigatorio("E-mail").trim().lowercase(Locale.ROOT)
        val outraConta = usuarios.buscarPorEmail(email)
        if (outraConta != null && outraConta.id != usuarioId) conflito("E-mail já cadastrado.")
        usuario.nome = nome
        usuario.email = email
        return usuario.paraResposta()
    }

    @Transactional
    fun alterarSenha(usuarioId: Long, dados: SenhaEntrada) {
        val novaSenha = dados.novaSenha.obrigatorio("Nova senha")
        validarSenha(novaSenha)
        val usuario = buscar(usuarioId)
        val senhaAtual = dados.senhaAtual.orEmpty()
        if (
            senhaAtual.toByteArray(Charsets.UTF_8).size > 72 ||
                !senhas.matches(senhaAtual, usuario.senhaHash)
        )
            invalido("Senha atual incorreta.")
        usuario.senhaHash = senhas.encode(novaSenha).obrigatorio("Hash da senha")
        usuario.versaoSessao++
        registrador.info("Senha atualizada e sessões encerradas: usuarioId={}", usuarioId)
    }

    @Transactional
    fun sair(usuarioId: Long) {
        buscar(usuarioId).versaoSessao++
    }
}
