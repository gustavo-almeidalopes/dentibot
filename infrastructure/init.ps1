<#
.SYNOPSIS
    Sobe a infraestrutura local e aplica as migrations. Ambiente é Windows.

.DESCRIPTION
    Um comando para sair do zero até a API pronta para rodar:
      1. sobe Postgres, Redis e MinIO;
      2. espera ficarem saudáveis de verdade (healthcheck, não sleep);
      3. roda o Flyway com o role migrador;
      4. confere o invariante de RLS antes de devolver o prompt.

    O passo 4 existe porque um banco com migration aplicada e RLS incompleto
    parece saudável por fora. Ver docs/architecture/tenancy.md.

.EXAMPLE
    ./infrastructure/init.ps1
    ./infrastructure/init.ps1 -Recriar   # apaga os volumes e começa do zero
#>
[CmdletBinding()]
param(
    [switch]$Recriar
)

# NÃO usar $ErrorActionPreference = 'Stop' aqui. No PowerShell 5.1, qualquer
# linha que um executável nativo escreve em stderr vira um ErrorRecord
# terminante — e `docker compose` escreve o progresso normal em stderr. O
# script abortaria em "Container X Running". O controle de erro é por
# $LASTEXITCODE, que é o que realmente indica falha.
$ErrorActionPreference = 'Continue'
$raiz = Split-Path -Parent $PSScriptRoot
$compose = Join-Path $PSScriptRoot 'docker-compose.yml'

function Etapa($texto) { Write-Host "`n=== $texto ===" -ForegroundColor Cyan }
function Ok($texto)    { Write-Host "  OK  $texto" -ForegroundColor Green }
function Falha($texto) { Write-Host "  ERRO  $texto" -ForegroundColor Red }

Etapa 'Verificando o Docker'
docker info --format '{{.ServerVersion}}' 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) {
    Falha 'Docker não está rodando. Abra o Docker Desktop e tente de novo.'
    exit 1
}
Ok 'daemon respondendo'

if ($Recriar) {
    Etapa 'Removendo volumes (-Recriar)'
    docker compose -f $compose down -v
    Ok 'volumes removidos'
}

Etapa 'Subindo Postgres, Redis e MinIO'
docker compose -f $compose up -d
if ($LASTEXITCODE -ne 0) { Falha 'docker compose up falhou'; exit 1 }

Etapa 'Esperando os healthchecks'
$prazo = (Get-Date).AddMinutes(3)
$naoSaudaveis = @()
do {
    # Consulta o estado de saúde de cada container pelo nome: o `ps --format
    # json` do compose muda de forma entre versões, e depender dele torna o
    # script frágil sem necessidade.
    $naoSaudaveis = @('dentibot-postgres','dentibot-redis','dentibot-minio') | Where-Object {
        (docker inspect -f '{{.State.Health.Status}}' $_ 2>$null) -ne 'healthy'
    }
    if ($naoSaudaveis.Count -eq 0) { break }
    Start-Sleep -Seconds 3
} while ((Get-Date) -lt $prazo)

if ($naoSaudaveis.Count -gt 0) {
    Falha "serviços ainda não saudáveis: $($naoSaudaveis -join ', ')"
    docker compose -f $compose logs --tail 30
    exit 1
}
Ok 'postgres, redis e minio saudáveis'

Etapa 'Aplicando migrations (Flyway, como dentibot_migrador)'

# JAVA_HOME costuma estar definido na máquina mas ausente NESTE processo, quando
# o terminal foi aberto antes de o JDK ser instalado. Em vez de mandar "abra um
# terminal novo", o script resolve sozinho.
if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME 'bin/java.exe'))) {
    $daMaquina = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'Machine')
    if ($daMaquina -and (Test-Path (Join-Path $daMaquina 'bin/java.exe'))) {
        $env:JAVA_HOME = $daMaquina
    } else {
        $adoptium = Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -ErrorAction SilentlyContinue |
            Where-Object { Test-Path (Join-Path $_.FullName 'bin/java.exe') } |
            Sort-Object Name -Descending | Select-Object -First 1
        if ($adoptium) { $env:JAVA_HOME = $adoptium.FullName }
    }
}
if (-not $env:JAVA_HOME) {
    Falha 'JDK 25 não encontrado. Instale com: winget install EclipseAdoptium.Temurin.25.JDK'
    exit 1
}
Ok "JDK em $env:JAVA_HOME"

Push-Location (Join-Path $raiz 'api-java')
try {
    $env:DENTIBOT_DB_URL = 'jdbc:postgresql://localhost:5433/dentibot'
    $env:DENTIBOT_DB_MIGRADOR_USER = 'dentibot_migrador'
    $env:DENTIBOT_DB_MIGRADOR_PASSWORD = 'migrador_local_apenas'
    $env:DENTIBOT_DB_USER = 'dentibot_app'
    $env:DENTIBOT_DB_PASSWORD = 'app_local_apenas'

    & ./mvnw.cmd -q -B flyway:migrate `
        "-Dflyway.url=$env:DENTIBOT_DB_URL" `
        "-Dflyway.user=$env:DENTIBOT_DB_MIGRADOR_USER" `
        "-Dflyway.password=$env:DENTIBOT_DB_MIGRADOR_PASSWORD" `
        "-Dflyway.schemas=plataforma" `
        "-Dflyway.defaultSchema=plataforma" `
        "-Dflyway.createSchemas=true" `
        "-Dflyway.locations=filesystem:src/main/resources/db/migration"
    if ($LASTEXITCODE -ne 0) { Falha 'flyway:migrate falhou'; exit 1 }
    Ok 'migrations aplicadas'
} finally {
    Pop-Location
}

Etapa 'Conferindo o invariante de RLS'
# Toda tabela com id_clinica precisa de ENABLE + FORCE + política. Um banco com
# migration aplicada e RLS incompleto parece saudável por fora — foi assim que a
# V1 rodou com vazamento entre clínicas.
$consulta = @'
WITH t AS (
  SELECT c.oid, n.nspname s, c.relname r, c.relrowsecurity, c.relforcerowsecurity
  FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
  WHERE c.relkind='r' AND n.nspname IN ('clinicas','identidade','pacientes','agenda',
        'prontuario','orcamento','financeiro','billing','estoque','lgpd','auditoria','plataforma'))
SELECT count(*) FROM t
WHERE EXISTS (SELECT 1 FROM pg_attribute a WHERE a.attrelid=t.oid
              AND a.attname IN ('id_clinica','clinica_id') AND a.attnum>0 AND NOT a.attisdropped)
  AND (NOT relrowsecurity OR NOT relforcerowsecurity
       OR NOT EXISTS (SELECT 1 FROM pg_policies p WHERE p.schemaname=t.s AND p.tablename=t.r
                      AND p.policyname='tenant_isolation'));
'@

$violacoes = docker exec -e PGPASSWORD=migrador_local_apenas dentibot-postgres `
    psql -U dentibot_migrador -h 127.0.0.1 -d dentibot -tA -c $consulta

if ([int]$violacoes -ne 0) {
    Falha "$violacoes tabela(s) com id_clinica sem proteção completa de RLS."
    exit 1
}
Ok 'toda tabela com id_clinica tem RLS + FORCE + tenant_isolation'

Write-Host "`nPronto." -ForegroundColor Green

# As variáveis que este script usou valem só DENTRO dele. Num terminal novo o
# `spring-boot:run` falha primeiro em DENTIBOT_DB_URL e, depois de resolvida
# essa, em DENTIBOT_CLERK_ISSUER — duas mensagens em sequência, cada uma
# parecendo o problema todo. Quatro linhas prontas custam menos que descobrir
# as duas na ordem.
Write-Host "`nCole no terminal onde a API vai rodar:" -ForegroundColor Cyan
Write-Host '  $env:DENTIBOT_DB_URL = "jdbc:postgresql://localhost:5433/dentibot"'
Write-Host '  $env:DENTIBOT_DB_PASSWORD = "app_local_apenas"'
Write-Host '  $env:DENTIBOT_DB_MIGRADOR_PASSWORD = "migrador_local_apenas"'
Write-Host '  $env:DENTIBOT_CLERK_ISSUER = "https://SUA-INSTANCIA.clerk.accounts.dev"'
# O issuer tem de ser o par da publishable key: chave de uma instância com
# emissor de outra devolve 401 em tudo, sem pista de qual das duas está errada.
# O host está dentro da própria chave — o trecho depois de `pk_test_` é o
# base64 de "<host>$".
Write-Host "  (o host é o base64 dentro da VITE_CLERK_PUBLISHABLE_KEY)" -ForegroundColor DarkGray

Write-Host "`n  API:      cd api-java; ./mvnw spring-boot:run"
Write-Host "  Landing:  cd web; npm run dev"
Write-Host "  SPA:      cd app; npm run dev"
Write-Host "  MinIO:    http://localhost:9001  (dentibot_local / dentibot_local_apenas)"
