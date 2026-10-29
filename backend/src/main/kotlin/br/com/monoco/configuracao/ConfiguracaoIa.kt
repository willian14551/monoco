package br.com.monoco.configuracao

import br.com.monoco.conta.*
import jakarta.persistence.*

@Entity
@Table(name = "configuracoes_ia")
class ConfiguracaoIa(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", unique = true)
    var usuario: Usuario = Usuario(),
    var provedor: String = "Demonstracao",
    var modelo: String = "",
    @Column(columnDefinition = "TEXT") var chaveCifrada: String? = null,
    var finalChave: String? = null,
    var tom: String = "Aventura",
    var tamanhoResposta: String = "Média",
    var criatividade: Double = 0.7,
)
