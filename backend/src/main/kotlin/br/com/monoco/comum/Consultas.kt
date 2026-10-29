package br.com.monoco.comum

import br.com.monoco.campanha.*
import br.com.monoco.campanha.Campanha
import br.com.monoco.conta.*
import br.com.monoco.conta.Usuario
import jakarta.persistence.criteria.Predicate
import java.util.Locale
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification

data class Pagina<T>(
    val conteudo: List<T>,
    val pagina: Int,
    val tamanho: Int,
    val total: Long,
    val ultima: Boolean,
)

fun <Entidade : Any, Resposta> Page<Entidade>.paraPagina(converter: (Entidade) -> Resposta) =
    Pagina(content.map(converter), number, size, totalElements, isLast)

fun <Valor : Any> Valor?.obrigatorio(nomeCampo: String): Valor =
    this ?: invalido("$nomeCampo é obrigatório.")

fun criarPaginacao(
    pagina: Int,
    tamanho: Int,
    ordenarPor: String,
    direcao: String,
    camposPermitidos: Set<String>,
): PageRequest {
    if (pagina < 0 || tamanho !in 1..100)
        invalido("Página deve ser maior ou igual a zero e tamanho deve estar entre 1 e 100.")
    if (ordenarPor !in camposPermitidos || direcao !in setOf("asc", "desc"))
        invalido("Ordenação inválida.")
    val ordenacao =
        Sort.by(if (direcao == "asc") Sort.Direction.ASC else Sort.Direction.DESC, ordenarPor)
    return PageRequest.of(
        pagina,
        tamanho,
        if (ordenarPor == "id") ordenacao else ordenacao.and(Sort.by("id")),
    )
}

fun <Entidade : Any> criarFiltro(
    usuarioId: Long? = null,
    busca: String = "",
    campo: String = "titulo",
    genero: String? = null,
    campanhaId: Long? = null,
    ativa: Boolean? = null,
): Specification<Entidade> = Specification { entidade, _, criterios ->
    val condicoes = mutableListOf<Predicate>()
    if (usuarioId != null)
        condicoes += criterios.equal(entidade.get<Usuario>("usuario").get<Long>("id"), usuarioId)
    if (campanhaId != null)
        condicoes += criterios.equal(entidade.get<Campanha>("campanha").get<Long>("id"), campanhaId)
    if (ativa != null) condicoes += criterios.equal(entidade.get<Boolean>("ativa"), ativa)
    if (busca.length > 150) invalido("A busca deve ter no máximo 150 caracteres.")
    if (busca.isNotBlank()) {
        val texto =
            busca
                .trim()
                .lowercase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
        condicoes += criterios.like(criterios.lower(entidade.get(campo)), "%$texto%", '\\')
    }
    if (!genero.isNullOrBlank())
        condicoes +=
            criterios.equal(criterios.lower(entidade.get("genero")), genero.lowercase(Locale.ROOT))
    criterios.and(*condicoes.toTypedArray())
}
