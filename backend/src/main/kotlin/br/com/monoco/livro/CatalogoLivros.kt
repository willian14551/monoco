package br.com.monoco.livro

val livrosIniciais =
    listOf(
        Livro(
            id = 1,
            titulo = "O Farol das Marés",
            genero = "Fantasia",
            descricao = "Um farol apagado. Uma ilha que desaparece. Uma última travessia.",
            premissa =
                "Na vila de Salmar, a luz do farol se apagou há três noites. " +
                    "Barcos voltam vazios e a cartógrafa Lia encontrou uma ilha que não deveria existir. " +
                    "Investigue o cais, negocie uma travessia e decida o destino do farol.",
            regras =
                "Fantasia de exploração. A personagem Lia é uma cartógrafa, não uma inimiga. " +
                    "Ofereça escolhas com consequências. Não decida ações pelo jogador. " +
                    "Resolva conflitos narrativamente; não use um sistema oficial de RPG.",
            abertura =
                "O sino do cais toca quando você chega a Salmar. Do outro lado da baía, " +
                    "o farol permanece escuro. Uma mulher abre um mapa molhado sobre um barril. " +
                    "\"Sou Lia. Esta ilha não estava aqui ontem. Preciso de alguém disposto a atravessar.\" " +
                    "Ela aponta para três caminhos: os pescadores, o barco abandonado e a trilha do farol. O que você faz?",
            duracao = "Aventura curta",
        ),
        Livro(
            id = 2,
            titulo = "A Última Estação",
            genero = "Mistério",
            descricao = "O trem chegou no horário. Todos os passageiros desapareceram.",
            premissa =
                "A estação de Véspera está fechada há vinte anos, mas um trem acaba de parar ali. " +
                    "Um bilhete com seu nome liga você ao maquinista Elias e ao relógio parado da plataforma.",
            regras =
                "Mistério investigativo sem violência gráfica. Elias sabe parte do segredo. " +
                    "Distribua pistas verificáveis. Não resolva o mistério sem as decisões do jogador.",
            abertura =
                "A chuva silencia quando o trem freia. A plataforma está vazia, exceto por " +
                    "um homem com uma lanterna. \"Você recebeu o bilhete?\", pergunta Elias. " +
                    "Atrás dele, o relógio marca 23h17. Dentro do trem, alguém bate no vidro. O que você faz?",
            duracao = "Investigação curta",
        ),
        Livro(
            id = 3,
            titulo = "Ecos de Órbita",
            genero = "Ficção científica",
            descricao = "A estação espacial responde. A tripulação, não.",
            premissa =
                "A estação Íris envia um pedido de resgate com a voz de alguém que você conhece. " +
                    "A engenheira Nara mantém o suporte de vida funcionando enquanto uma segunda transmissão começa.",
            regras =
                "Ficção científica de exploração. Recursos são limitados, mas toda falha deve " +
                    "oferecer alternativas. Nara é uma aliada. Evite controlar o personagem do jogador.",
            abertura =
                "A escotilha da Íris se abre com um estalo. Luzes de emergência desenham " +
                    "uma trilha até a engenharia. Pelo rádio, Nara sussurra: \"Não siga a voz do corredor. " +
                    "Eu estou na sala de máquinas.\" Dois sinais piscam no seu visor. Qual caminho você escolhe?",
            duracao = "Missão curta",
        ),
    )

@org.springframework.stereotype.Component
class PrepararCatalogo(private val livros: LivroRepositorio) :
    org.springframework.boot.ApplicationRunner {
    @org.springframework.transaction.annotation.Transactional
    override fun run(args: org.springframework.boot.ApplicationArguments) {
        livrosIniciais.forEach { if (!livros.existsById(it.id)) livros.save(it) }
    }
}
