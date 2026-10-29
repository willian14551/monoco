package br.com.monoco.campanha

import br.com.monoco.conta.*
import br.com.monoco.livro.*
import br.com.monoco.personagem.*
import jakarta.persistence.*

@Entity
@Table(name = "campanhas")
class Campanha(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    var usuario: Usuario = Usuario(),
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "personagem_id")
    var personagem: Personagem = Personagem(),
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "livro_id")
    var livro: Livro = Livro(),
    var titulo: String = "",
    var ultimaAtividade: Long = System.currentTimeMillis(),
    var geracaoId: String? = null,
    var geracaoInicio: Long? = null,
)
