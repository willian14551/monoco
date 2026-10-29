package br.com.monoco.conta

import br.com.monoco.personagem.*
import jakarta.persistence.*

@Entity
@Table(name = "usuarios")
class Usuario(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(nullable = false, length = 100) var nome: String = "",
    @Column(nullable = false, unique = true, length = 254) var email: String = "",
    @Column(nullable = false, length = 100) var senhaHash: String = "",
    var versaoSessao: Int = 0,
    @OneToMany(mappedBy = "usuario") var personagens: MutableList<Personagem> = mutableListOf(),
)
