package br.com.monoco.campanha

import jakarta.persistence.*

@Entity
@Table(name = "mensagens")
class Mensagem(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campanha_id")
    var campanha: Campanha = Campanha(),
    var autor: String = "jogador",
    @Column(columnDefinition = "TEXT") var texto: String = "",
    var criadaEm: Long = System.currentTimeMillis(),
    var interrompida: Boolean = false,
    var requisicaoId: String? = null,
)
