$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
if (Test-Path .env) { Write-Host "O arquivo .env já existe. Ele foi preservado."; exit 0 }
function Novo-Segredo {
    $bytes = New-Object byte[] 32
    $gerador = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $gerador.GetBytes($bytes) } finally { $gerador.Dispose() }
    return [Convert]::ToBase64String($bytes)
}
@"
DB_PASSWORD=$(Novo-Segredo)
DB_ROOT_PASSWORD=$(Novo-Segredo)
JWT_SECRET=$(Novo-Segredo)
ENCRYPTION_KEY=$(Novo-Segredo)
DEMO_PERMITIDA=true
"@ | Set-Content -Encoding ASCII .env
Write-Host "Configuração criada. Agora execute: docker compose up --build"
