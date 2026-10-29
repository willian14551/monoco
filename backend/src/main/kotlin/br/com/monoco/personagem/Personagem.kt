package br.com.monoco.personagem

import br.com.monoco.conta.*
import jakarta.persistence.*

@Entity
@Table(name = "personagens")
class Personagem(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    var usuario: Usuario = Usuario(),
    var nome: String = "",
    var classe: String = "",
    @Column(columnDefinition = "TEXT") var atributos: String = "",
    @Column(columnDefinition = "TEXT") var historia: String = "",
    @Column(columnDefinition = "TEXT") var segredos: String = "",
    var compartilharNome: Boolean = true,
    var compartilharClasse: Boolean = true,
    var compartilharAtributos: Boolean = true,
    var compartilharHistoria: Boolean = true,
    var compartilharSegredos: Boolean = true,
)
