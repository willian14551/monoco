package br.com.monoco.memoria

import br.com.monoco.campanha.*
import jakarta.persistence.*

@Entity
@Table(name = "memorias")
class Memoria(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campanha_id")
    var campanha: Campanha = Campanha(),
    var titulo: String = "",
    @Column(columnDefinition = "TEXT") var texto: String = "",
    var ativa: Boolean = true,
)
