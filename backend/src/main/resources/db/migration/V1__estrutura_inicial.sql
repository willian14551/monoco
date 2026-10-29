CREATE TABLE usuarios (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 nome VARCHAR(100) NOT NULL,
 email VARCHAR(254) NOT NULL UNIQUE,
 senha_hash VARCHAR(100) NOT NULL,
 versao_sessao INT NOT NULL DEFAULT 0
);
CREATE TABLE configuracoes_ia (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 usuario_id BIGINT NOT NULL UNIQUE,
 provedor VARCHAR(30) NOT NULL,
 modelo VARCHAR(150) NOT NULL,
 chave_cifrada TEXT,
 final_chave VARCHAR(4),
 tom VARCHAR(30) NOT NULL,
 tamanho_resposta VARCHAR(15) NOT NULL,
 criatividade DOUBLE NOT NULL,
 CONSTRAINT fk_config_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);
CREATE TABLE personagens (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 usuario_id BIGINT NOT NULL,
 nome VARCHAR(100) NOT NULL,
 classe VARCHAR(100) NOT NULL,
 atributos TEXT NOT NULL,
 historia TEXT NOT NULL,
 segredos TEXT NOT NULL,
 compartilhar_nome BOOLEAN NOT NULL,
 compartilhar_classe BOOLEAN NOT NULL,
 compartilhar_atributos BOOLEAN NOT NULL,
 compartilhar_historia BOOLEAN NOT NULL,
 compartilhar_segredos BOOLEAN NOT NULL,
 CONSTRAINT fk_personagem_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);
CREATE INDEX idx_personagens_usuario ON personagens(usuario_id);
CREATE TABLE livros (
 id BIGINT PRIMARY KEY,
 titulo VARCHAR(150) NOT NULL,
 genero VARCHAR(60) NOT NULL,
 descricao TEXT NOT NULL,
 premissa TEXT NOT NULL,
 regras TEXT NOT NULL,
 abertura TEXT NOT NULL,
 duracao VARCHAR(80) NOT NULL
);
CREATE TABLE campanhas (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 usuario_id BIGINT NOT NULL,
 personagem_id BIGINT NOT NULL,
 livro_id BIGINT NOT NULL,
 titulo VARCHAR(150) NOT NULL,
 ultima_atividade BIGINT NOT NULL,
 geracao_id VARCHAR(36),
 geracao_inicio BIGINT,
 CONSTRAINT fk_campanha_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
 CONSTRAINT fk_campanha_personagem FOREIGN KEY (personagem_id) REFERENCES personagens(id),
 CONSTRAINT fk_campanha_livro FOREIGN KEY (livro_id) REFERENCES livros(id)
);
CREATE INDEX idx_campanhas_usuario ON campanhas(usuario_id);
CREATE TABLE mensagens (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 campanha_id BIGINT NOT NULL,
 autor VARCHAR(10) NOT NULL,
 texto TEXT NOT NULL,
 criada_em BIGINT NOT NULL,
 interrompida BOOLEAN NOT NULL,
 requisicao_id VARCHAR(36),
 CONSTRAINT fk_mensagem_campanha FOREIGN KEY (campanha_id) REFERENCES campanhas(id) ON DELETE CASCADE,
 CONSTRAINT uk_mensagem_requisicao UNIQUE(campanha_id, requisicao_id)
);
CREATE INDEX idx_mensagens_campanha ON mensagens(campanha_id, id);
CREATE TABLE memorias (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 campanha_id BIGINT NOT NULL,
 titulo VARCHAR(120) NOT NULL,
 texto TEXT NOT NULL,
 ativa BOOLEAN NOT NULL,
 CONSTRAINT fk_memoria_campanha FOREIGN KEY (campanha_id) REFERENCES campanhas(id) ON DELETE CASCADE
);
CREATE INDEX idx_memorias_campanha ON memorias(campanha_id);
