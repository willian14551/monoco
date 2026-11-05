# Monoco: guia de execução e apresentação

Roteiro de consulta para entender o projeto e apresentar os requisitos de mobile e backend. Os arquivos do app ficam em `app/src/main/java/com/example/monoco/`; os do servidor, em `backend/src/main/kotlin/br/com/monoco/`.

## 1. Colocar o projeto para funcionar

Use o caminho principal: **Docker + MySQL, porta 8081**.

1. Abra o Docker Desktop. Na raiz do projeto, execute `iniciar-backend.bat`.
2. Aguarde **Backend pronto com MySQL**. A primeira compilação pode demorar para baixar dependências. O iniciador espera a saúde da API e do banco antes de anunciar que terminou.
3. No computador, abra `http://localhost:8081/api/saude`. O JSON deve conter `status: ok` e `servico: monoco`.
4. Abra a raiz do projeto no Android Studio, execute o módulo `app` em um emulador e cadastre uma conta. O app verifica a conexão automaticamente ao abrir.

| Local de acesso | Endereço da API |
| --- | --- |
| Navegador do computador | `http://localhost:8081/api/` |
| Emulador Android | `http://10.0.2.2:8081/api/` |
| Celular físico na mesma rede | `http://IP_DO_COMPUTADOR:8081/api/` |

No celular, informe o IP do computador em **Servidor** e permita a porta 8081 na rede privada do Windows. Um endereço salvo anteriormente é preservado: use **Usar endereço padrão → Salvar e verificar** para restaurar a configuração do emulador.

O container `api` executa o Spring Boot; `banco` executa o MySQL. O mapeamento `8081:8080` liga a porta 8081 do computador à porta 8080 do container. O app utiliza 8081.

`preparar-ambiente.ps1` cria o `.env` com senhas e chaves aleatórias. Ele preserva um arquivo existente. O volume `dados_mysql` mantém os registros entre reinícios. Para conferir os serviços, execute `docker compose ps` dentro de `backend/`; para parar, `docker compose stop`.

Os scripts `iniciar-local.bat` e `iniciar-local.sh` oferecem uma alternativa com H2 na porta 8080. São outro ambiente e outro banco. O processo `bootRun` fica aberto atendendo requisições; a mensagem `Started MonocoBackendKt` significa que está pronto, mesmo que o Gradle continue em execução.

Se a saúde não abrir, confira `docker compose ps` e `docker compose logs --tail 80 api`. Se abrir no computador e falhar no app, confira o endereço do dispositivo. Uma conta criada no H2 e uma conta criada no MySQL pertencem a bancos diferentes.

## 2. O caminho de uma ação no aplicativo

O Monoco permite criar personagens, escolher um livro de aventura, iniciar uma campanha e enviar ações ao narrador. Memórias guardam fatos que devem acompanhar a próxima narração.

| Arquivo | Responsabilidade |
| --- | --- |
| `MainActivity.kt` | Abre o aplicativo e recebe API e sessão por Hilt. |
| `AutenticacaoApp.kt` | Verifica servidor e sessão; controla cadastro, login e entrada na área autenticada. |
| `MonocoApp.kt` | Define abas e rotas com Navigation Compose e obtém o ViewModel. |
| `ui/telas/` | Desenha os formulários e listas com Compose e Material 3; recebe ações do usuário. |
| `ui/MonocoViewModel.kt` | Coordena operações, estados de carregamento, erros e narração. |
| `dados/RepositorioMonoco.kt` | Coordena chamadas à API e sincronização dos dados locais. |
| `dados/remoto/ApiMonoco.kt` | Faz HTTP, envia JSON e token e interpreta respostas e eventos do chat. |
| `dados/local/` | Contém entidades, banco Room e DAO do Android. |

Ao salvar uma ficha, `TelaEditarPersonagem` valida o nome e chama `aoSalvar`. `MonocoApp` liga essa ação ao `MonocoViewModel`, que chama o repositório. O repositório solicita o salvamento à API e sincroniza o Room. O DAO emite os dados por `Flow`, o ViewModel os expõe no estado e o Compose atualiza a tela.

**Hilt:** `AplicacaoMonoco` usa `@HiltAndroidApp`; `MainActivity`, `@AndroidEntryPoint`; o ViewModel, `@HiltViewModel` e construtor `@Inject`. `ModuloDados` fornece banco e narrador. `hiltViewModel()` obtém a instância gerenciada. API e sessão são `@Singleton`; banco e repositório acompanham o ViewModel com `@ViewModelScoped`. O módulo registra o fechamento do banco quando o ViewModel é encerrado.

Essa organização é MVVM: as telas apresentam o estado, o ViewModel coordena as ações e o repositório acessa os dados. As chamadas de rede executam em `Dispatchers.IO` por corrotinas. O cadastro e o login usam a API pela área de autenticação; algumas operações de conta e configuração também chamam a API a partir do ViewModel.

O Room é a cópia persistente do Android, separada por conta e endereço do servidor. As alterações da área conectada passam primeiro pela API. Ter dados em cache não transforma o aplicativo em um sistema completo de edição offline.

## 3. Como o backend organiza os dados

O backend é uma API HTTP em Kotlin com Spring Boot. Ele identifica o usuário, valida o pedido, aplica as regras e acessa o banco. O Spring fornece as dependências pelos construtores; o Hilt é utilizado no Android.

| Camada e exemplo | O que faz |
| --- | --- |
| `personagem/PersonagemControlador.kt` | Recebe a rota, o corpo JSON e os parâmetros; aciona o serviço. |
| `personagem/PersonagemServico.kt` | Aplica regras, verifica o dono da ficha e protege personagens vinculados a campanhas. |
| `personagem/PersonagemRepositorio.kt` | Acessa o banco por Spring Data JPA e consultas JPQL. |
| `personagem/Personagem.kt` | Representa a entidade persistida. |
| `personagem/DadosApi.kt` | Define DTOs de entrada e saída, sem expor toda a entidade. |

Ao criar um personagem, o app envia `POST /api/personagens`. A segurança identifica o usuário. O controlador recebe o DTO, o serviço monta a entidade e o repositório chama `save`. O servidor retorna o DTO da ficha com status **201**. A atualização usa `PUT /api/personagens/{id}`; a exclusão, `DELETE`; a consulta, `GET`.

| Entidade | Papel e relacionamento |
| --- | --- |
| Usuário | Conta dona dos personagens e campanhas. |
| Configuração de IA | Relação **1:1** com usuário, por `@OneToOne` e coluna única. |
| Personagem | Um usuário pode ter vários personagens: relação **1:N**. |
| Livro | Um livro pode ser usado por várias campanhas: relação **1:N**. |
| Campanha | Vincula usuário, personagem e livro; um personagem pode participar de várias campanhas. |
| Mensagem | Pertence a uma campanha, que pode possuir várias mensagens. |
| Memória | Pertence a uma campanha, que pode possuir várias memórias. |

O lado N das relações 1:N utiliza `@ManyToOne` e `@JoinColumn`. A criação da campanha associa `personagemId` e `livroId`; o servidor confere a propriedade do personagem. Excluir campanha remove suas mensagens e memórias pelas restrições do banco. Excluir uma ficha ainda vinculada a campanhas retorna **409**.

O Flyway aplica o SQL de `backend/src/main/resources/db/migration/V1__estrutura_inicial.sql` para criar as tabelas. Depois, o Hibernate valida a estrutura. O catálogo de livros é preparado na inicialização. Esse fluxo dispensa criar tabelas manualmente antes de usar o app.

Para demonstrar filtro, ordenação e paginação, consulte `GET /api/personagens?busca=Aris&ordenarPor=nome&direcao=asc&pagina=0&tamanho=10`. `comum/Consultas.kt` centraliza filtros e paginação. Os parâmetros chegam ao controlador por `@RequestParam`; o identificador na rota usa `@PathVariable`.

## 4. Autenticação, validação, narração e testes

No login, `ContaServico` procura o e-mail e compara a senha com o hash BCrypt. `seguranca/Tokens.kt` emite um JWT assinado, com usuário, expiração e versão da sessão. O app guarda a sessão em `SessaoSegura`, protegida por AES-GCM e Android Keystore, e envia `Authorization: Bearer TOKEN` nas chamadas autenticadas.

`FiltroToken`, em `seguranca/Seguranca.kt`, valida o token e identifica o usuário. Os serviços e repositórios conferem a propriedade dos dados. No logout ou na alteração de senha, a versão da sessão é incrementada, invalidando os tokens anteriores. O JWT é assinado; a codificação Base64 do seu conteúdo não equivale a criptografia.

No `.env`, `JWT_SECRET` assina tokens, `ENCRYPTION_KEY` protege as chaves de IA e `DB_PASSWORD` autentica no MySQL. A chave OpenAI ou OpenRouter é configurada separadamente por usuário, na tela de IA.

| Resultado | Exemplo |
| --- | --- |
| **200** | Consulta ou atualização concluída. |
| **201** | Personagem, campanha ou memória criada. |
| **204** | Exclusão ou operação concluída sem corpo de resposta. |
| **400** | Campo obrigatório ausente, JSON ou parâmetro inválido. |
| **401** | Credencial incorreta, token ausente, inválido ou expirado. |
| **404** | Recurso não encontrado ou pertencente a outra conta. |
| **409** | E-mail duplicado ou personagem vinculado a uma campanha. |

DTOs usam validações Jakarta e `@Valid`; regras de negócio também são verificadas nos serviços. `comum/Erros.kt` converte falhas para `ProblemDetail`, incluindo `detail` e erros por campo. Nas rotas JSON, falhas inesperadas retornam 500 sem expor detalhes internos. `@Transactional` mantém operações relacionadas na mesma transação.

No chat, a ação do jogador é salva antes da geração. `narracao/ChatServico.kt` reúne livro, campos compartilhados, memórias ativas e mensagens recentes. `ProvedorHttp`, em `ProvedorIa.kt`, chama OpenAI ou OpenRouter. A resposta chega em trechos por SSE e é gravada ao concluir. Falhas durante o fluxo chegam no evento `erro`, e a ação continua salva. O modo Demonstração utiliza resposta roteirizada e permite apresentar o fluxo sem uma chave de IA.

Logs de requisições ficam em `RegistroRequisicoes.kt`; serviços registram eventos e erros. Consulte `docker compose logs --tail 80 api`. Os testes unitários usam JUnit e MockK; os de integração usam Spring, MockMvc e H2 em memória. Execute `gradlew.bat check` dentro de `backend/`. O JaCoCo exige 90% das linhas de três serviços e gera o relatório em `build/reports/jacoco/test/html/index.html`. Essa cobertura é dos serviços selecionados, não uma garantia de ausência de bugs.

## 5. Roteiro da demonstração

Prepare uma conta de apresentação e use dados descartáveis nos CRUDs. Abra também o Swagger em `http://localhost:8081/api/swagger-ui.html`.

1. **Execução:** mostre a mensagem de backend pronto e o `GET /saude` no navegador. Explique API, banco e porta 8081.
2. **Cadastro e login:** mostre um campo inválido, corrija e entre. Explique que o servidor valida novamente os dados e emite o token.
3. **CRUD de personagem:** crie **Aris**, classe **Guerreiro**; mostre a ficha na lista; altere para **Mago**; exclua e confirme que desapareceu. Use uma ficha sem campanha para concluir as quatro operações.
4. **Associação:** crie outro personagem, escolha um livro e inicie uma campanha. Mostre que o título, o personagem e o livro ficam ligados, e que a abertura já aparece no chat.
5. **CRUD de memória:** dentro da campanha, abra Memórias. Crie **Aliada**, com texto **Lia ajuda o grupo**; consulte; edite o texto; alterne ativa/inativa; exclua. Desativar é uma atualização, pois o registro permanece no banco.
6. **Persistência:** feche e reabra o app, entre na mesma conta e mostre que os registros mantidos continuam disponíveis.
7. **Consulta e proteção:** pelo Swagger, faça login em `POST /sessoes`, copie o campo `token` e use **Authorize**. Consulte personagens com `busca`, `ordenarPor` e `direcao`. Tente excluir uma ficha vinculada a campanha para mostrar 409; remova a autorização e tente uma rota protegida para mostrar 401.
8. **Testes e código:** abra um teste de personagem e um de memória, o tratamento de erros, os relacionamentos e o relatório de cobertura. Mostre o resultado real da execução no seu ambiente.

| CRUD | Tela no app | Rota no backend |
| --- | --- | --- |
| Personagem: criar e listar | Personagens | `POST` e `GET /personagens` |
| Personagem: editar e excluir | Editar ficha / Excluir | `PUT` e `DELETE /personagens/{id}` |
| Memória: criar e listar | Campanha → Memórias | `POST` e `GET /campanhas/{id}/memorias` |
| Memória: editar e excluir | Editar / Excluir | `PUT` e `DELETE /campanhas/{id}/memorias/{memoriaId}` |

No Swagger, as rotas são relativas ao prefixo `/api`. Para criar uma ficha, um corpo mínimo é `{ "nome": "Aris", "classe": "Guerreiro" }`. Para uma memória, use `{ "titulo": "Aliada", "texto": "Lia ajuda o grupo", "ativa": true }` na campanha criada.

O CRUD completo exigido é demonstrado em **Personagem** e **Memória**. O catálogo de livros é de consulta. Campanhas oferecem criação, consulta e exclusão; a interface não apresenta edição de campanha. A narração por provedor real é uma demonstração adicional e utiliza a chave e os créditos configurados na conta.

## 6. Perguntas do professor e conferência da entrega

**Por que usar ViewModel?** Para coordenar as ações e expor o estado que as telas observam, separando a interface do acesso aos dados.

**Onde entra o Hilt?** Ele cria e entrega as dependências. O ViewModel recebe o repositório por construtor; a tela usa `hiltViewModel()`.

**Quem chama o backend?** `ApiMonoco` faz o HTTP. O repositório coordena CRUD e sincronização; autenticação e algumas configurações também chamam a API diretamente.

**Room e MySQL fazem a mesma coisa?** São bancos em lugares diferentes. Room mantém a cópia no Android; MySQL é o banco do servidor no ambiente Docker.

**O que é um DTO?** Um objeto que define os dados de entrada ou saída da API. Ele evita expor a entidade inteira, incluindo senha, chave cifrada e relações internas.

**Quais relacionamentos foram usados?** 1:1 entre usuário e configuração; 1:N entre usuário e personagens, personagem e campanhas, campanha e mensagens ou memórias.

**Autenticação e autorização são iguais?** Autenticar identifica o usuário. Autorizar verifica se ele pode executar a ação sobre aquele recurso.

**Como um usuário é impedido de acessar outro?** O token identifica a conta e as consultas restringem o recurso ao usuário autenticado. Um ID conhecido não concede acesso.

**Por que validar na tela e no servidor?** A tela orienta o preenchimento. O servidor protege as regras mesmo quando alguém chama a API diretamente.

**Para que serve o CORS?** Define quais origens de páginas web podem chamar a API pelo navegador. A configuração fica em `Seguranca.kt`; a autenticação continua sendo exigida nas rotas protegidas.

**Por que o texto aparece aos poucos?** O servidor envia eventos SSE; `ApiMonoco.narrar()` recebe os trechos e o ViewModel atualiza o estado exibido no chat.

**Como a lista muda depois de salvar?** O repositório atualiza o Room; o `Flow` do DAO altera o estado observado pelo Compose.

**O que são testes unitários e de integração?** Os unitários isolam um serviço com dependências simuladas. Os de integração verificam várias camadas juntas, incluindo HTTP, segurança e banco de testes.

O projeto atende à opção de extensão de serviços: possui sete entidades, dois tipos de relacionamento, dois CRUDs completos, associação de entidades, consultas com filtro e ordenação, autenticação, logs e testes. Antes de enviar, complete no README **os nomes de todos os integrantes e o link do vídeo no YouTube**. Todos devem participar do vídeo, conforme o enunciado.

**Validação desta revisão:** 68 testes do backend, 31 chamadas HTTP e 14 verificações isoladas do endereço foram aprovados. A compilação Android ficou pendente por bloqueio HTTP 403 no download de dependências. Containers reais e interface em dispositivo devem ser conferidos no computador de desenvolvimento. O detalhamento está em `docs/VALIDACAO.md`.

Material de apoio utilizado: **Trabalho Final de Backend**, p. 1; **Back 02 - Conceitos Iniciais**, p. 23 e 45-59; **Back 03 - Validação e erros**, p. 8 e 13-30; **Back 05 - JPA - Relacionamentos**, p. 8-14; **Back 06 - Observabilidade e testes unitários**, p. 9-33; **Back 07 - Segurança com JWT**, p. 4-17. Os dois arquivos enviados de relacionamentos têm o mesmo conteúdo.
