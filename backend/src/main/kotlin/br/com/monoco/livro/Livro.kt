package br.com.monoco.livro

import jakarta.persistence.*

@Entity
@Table(name = "livros")
class Livro(
    @Id var id: Long = 0,
    var titulo: String = "",
    var genero: String = "",
    @Column(columnDefinition = "TEXT") var descricao: String = "",
    @Column(columnDefinition = "TEXT") var premissa: String = "",
    @Column(columnDefinition = "TEXT") var regras: String = "",
    @Column(columnDefinition = "TEXT") var abertura: String = "",
    var duracao: String = "",
)
