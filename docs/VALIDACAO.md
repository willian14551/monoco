# Validação da revisão de execução

Revisão realizada em 05/11/2026, partindo do commit `6d7894e8e366cd1fbacaba2b20ccc4353bb461fb`.

| Verificação | Resultado |
| --- | --- |
| Backend: `check bootJar` com Java 21 | Aprovado: 68 testes, nenhuma falha, nenhum teste ignorado e JAR gerado. |
| Cobertura de `ContaServico` | 96,7% das linhas. |
| Cobertura de `PersonagemServico` | 91,2% das linhas. |
| Cobertura de `MemoriaServico` | 97,3% das linhas. |
| API executada e acessada por HTTP | 31 chamadas aprovadas, incluindo cadastro, login, token, CRUDs de personagem e memória, associação da campanha, filtro e ordenação, erros, narração SSE e logout. |
| Persistência do servidor após reinício | Aprovada em banco H2 em arquivo temporário: a conta e o personagem continuaram disponíveis após encerrar e iniciar novamente o processo. |
| Normalização do endereço do app | 14 verificações aprovadas em JVM isolada, compilando o arquivo real `ConfiguracaoServidor.kt` e usando dependências já disponíveis, sem rede. |
| Iniciador PowerShell | Sintaxe validada e cinco cenários aprovados com comando Docker simulado e endpoint HTTP local real. |
| Compose e manifesto Android | Estruturas e referências validadas; o mapeamento 8081:8080 e os healthchecks correspondem ao endereço padrão. |
| APK e suíte Android completa | Pendente: a tentativa foi bloqueada no download de dependências do plugin Android com HTTP 403. |
| Containers Docker e aplicativo em dispositivo | Não executados neste ambiente, que não disponibiliza Docker Desktop, dispositivo ou emulador Android. |

Os cinco cenários do iniciador foram: inicialização com API correta e geração de segredos de 32 bytes; preservação do `.env` na reexecução; mecanismo Docker indisponível; falha de `docker compose`; e recusa de outro serviço respondendo na porta 8081. A simulação valida o fluxo do script, sem substituir a execução de containers reais.

Os testes da API utilizaram H2, e a narração utilizou o modo Demonstração. Não foram realizadas chamadas pagas a provedores de IA.

Para concluir a validação do Android no computador de desenvolvimento, execute na raiz:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin
```

Com emulador ou dispositivo conectado, execute também `:app:connectedDebugAndroidTest` e confira cadastro, login, os dois CRUDs e a alteração de servidor pela interface. Para conferir a execução real com MySQL, use `iniciar-backend.bat` e o roteiro do guia.
