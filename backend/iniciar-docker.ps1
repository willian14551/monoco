$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

try {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw "Instale o Docker Desktop e abra-o antes de iniciar o backend."
    }

    docker info --format '{{.ServerVersion}}' | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Abra o Docker Desktop e aguarde o mecanismo do Docker ficar pronto."
    }

    if (-not (Test-Path .env)) {
        & (Join-Path $PSScriptRoot "preparar-ambiente.ps1")
    }

    Write-Host "Iniciando API e MySQL. A primeira compilacao pode levar alguns minutos."
    docker compose up --build --detach --wait --wait-timeout 180
    if ($LASTEXITCODE -ne 0) {
        throw "Nao foi possivel iniciar os containers. Confira as portas 8081 e 3306 e execute: docker compose logs --tail 50 api"
    }

    $resposta = Invoke-RestMethod -Uri "http://localhost:8081/api/saude" -TimeoutSec 5
    if ($resposta.status -ne "ok" -or $resposta.servico -ne "monoco") {
        throw "A porta 8081 respondeu com um servico diferente do Monoco."
    }

    Write-Host "Backend pronto com MySQL. Os containers continuam funcionando em segundo plano."
    Write-Host "Saude: http://localhost:8081/api/saude"
    Write-Host "Swagger: http://localhost:8081/api/swagger-ui.html"
    Write-Host "Emulador Android: http://10.0.2.2:8081/api/"
    Write-Host "Agora execute o modulo app no Android Studio."
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}
