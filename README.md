# Monoco

Aplicativo Android de RPG narrativo, com Jetpack Compose, Material 3, Navigation Compose, Room, MVVM e Hilt. O backend usa Kotlin, Spring Boot, Spring Security com JWT, JPA e MySQL.

O caminho principal de execução é **Docker + MySQL na porta 8081**. O aplicativo de desenvolvimento já usa `http://10.0.2.2:8081/api/`, que permite ao emulador acessar o computador.

## Iniciar no Windows

1. Abra o Docker Desktop e aguarde o mecanismo ficar disponível.
2. Na raiz do projeto, execute **`iniciar-backend.bat`**. Ele prepara o `.env` quando necessário, compila a API, inicia API e MySQL e espera a verificação de saúde dos containers.
3. Aguarde a mensagem **`Backend pronto com MySQL`**. A primeira compilação baixa dependências e pode levar alguns minutos.
4. Abra a raiz do projeto no Android Studio, escolha o módulo **app**, selecione um emulador e clique em **Run**.
5. O app verifica a conexão automaticamente. Cadastre uma conta e faça login.

O script encerra a etapa de preparação com os containers funcionando em segundo plano. Use-o novamente quando precisar iniciar ou atualizar o backend.

| Acesso | Endereço |
| --- | --- |
| Verificação no navegador do computador | http://localhost:8081/api/saude |
| Swagger no computador | http://localhost:8081/api/swagger-ui.html |
| API no emulador Android | http://10.0.2.2:8081/api/ |
| API em celular na mesma rede | `http://IP_DO_COMPUTADOR:8081/api/` |

No celular físico, ajuste o endereço no botão **Servidor** antes do login. Use o IP local do computador e permita o acesso à porta 8081 na rede privada do Windows. `localhost` no celular identifica o próprio celular.

Um endereço salvo manualmente em uma instalação anterior é preservado. Para voltar ao padrão do emulador, abra **Servidor → Usar endereço padrão → Salvar e verificar**.

## Comandos do Docker

Execute os comandos abaixo dentro de `backend/`.

| Objetivo | Comando |
| --- | --- |
| Iniciar manualmente após preparar o `.env` | `docker compose up --build --detach --wait --wait-timeout 180` |
| Conferir os serviços | `docker compose ps` |
| Ver os últimos eventos da API | `docker compose logs --tail 80 api` |
| Parar os serviços | `docker compose stop` |

O arquivo `backend/preparar-ambiente.ps1` gera senhas e chaves aleatórias para o MySQL, JWT e criptografia. O `.env` é preservado quando já existe e está no `.gitignore`. O banco utiliza o volume `dados_mysql` e mantém os cadastros entre reinícios.

## Porta 8081 e alternativa local

O Docker publica `8081:8080`: o computador recebe as chamadas em 8081 e encaminha para 8080 dentro do container. O app acessa a porta do computador.

`backend/iniciar-local.bat` e `backend/iniciar-local.sh` são uma alternativa para desenvolvimento com Java 21 e H2 em arquivo na porta 8080. Esse modo utiliza outro banco. Para usá-lo, configure o app para `http://10.0.2.2:8080/api/`. O iniciador principal da raiz utiliza Docker e MySQL.

Ao executar `bootRun`, o processo continua aberto para atender requisições. A linha `Started MonocoBackendKt` indica que iniciou; o percentual do Gradle não representa uma instalação pendente.

## Demonstração e IA

Personagens e memórias oferecem CRUD completo. Campanhas vinculam personagem e livro, mantêm mensagens e memórias e podem ser excluídas. Os livros formam o catálogo de consulta.

O ambiente de demonstração permite respostas roteirizadas sem chave de IA. Para narração por um modelo, abra **Perfil → Configurações da IA**, escolha OpenAI ou OpenRouter e teste a conexão com modelo e chave próprios. Os CRUDs funcionam sem contratar ou configurar um provedor de IA.

## Testes

Com Java 21, dentro de `backend/`:

```powershell
.\gradlew.bat check bootJar
```

`check` executa os testes e verifica cobertura de pelo menos 90% das linhas de `ContaServico`, `PersonagemServico` e `MemoriaServico`. O relatório fica em `backend/build/reports/jacoco/test/html/index.html`.

Na raiz, com o SDK Android instalado:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin
```

Para executar os testes instrumentados, conecte um dispositivo ou inicie um emulador e use `:app:connectedDebugAndroidTest`. No Linux ou macOS, substitua `.\gradlew.bat` por `sh gradlew`.

Os resultados e limites da revisão de execução estão em [docs/VALIDACAO.md](docs/VALIDACAO.md).

## Guia e entrega acadêmica

O [guia de apresentação](docs/GUIA_APRESENTACAO.md) explica a execução, as camadas, Hilt, relacionamentos, autenticação, erros, testes, o roteiro dos dois CRUDs e perguntas do professor.

O projeto segue a opção de extensão de serviços do trabalho: sete entidades, relacionamentos 1:1 e 1:N, CRUD de personagens e memórias, associação na criação de campanha, consultas com filtro e ordenação, autenticação, logs e testes dos serviços.

Antes da entrega, complete estes dados:

- Integrantes: **preencher os nomes completos**.
- Vídeo no YouTube: **adicionar o link da apresentação**.

Todos os integrantes devem participar da apresentação, conforme o enunciado.
