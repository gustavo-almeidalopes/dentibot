# Segurança

O DentiBot guarda prontuário odontológico — dado pessoal sensível de saúde
(LGPD, art. 11). Vulnerabilidade aqui não é bug de estética: é exposição de
paciente.

## Como reportar

Pelo **reporte privado de vulnerabilidade do GitHub**, em
<https://github.com/gustavo-almeidalopes/dentibot/security/advisories/new>.
Não abra issue pública, não mande em grupo de WhatsApp, não teste contra dado
real de clínica.

O endereço também está em `/.well-known/security.txt` (RFC 9116), servido pelo
web.

## O que acontece depois

| Severidade (CVSS) | Primeira resposta | Correção no `main` |
| --- | --- | --- |
| Crítica (9,0–10) | 24 h | **72 h** |
| Alta (7,0–8,9) | 72 h | **7 dias** |
| Média (4,0–6,9) | 7 dias | 30 dias |
| Baixa (< 4,0) | 14 dias | próximo ciclo |

O mesmo prazo vale para CVE em dependência: o Dependabot abre o PR, o
`seguranca.yml` reprova dependência nova com CVE alta, e o `imagem.yml` não
publica imagem com CVE crítica corrigível (Trivy).

## Versões

Só o `main` recebe correção. Não há linha de versão antiga mantida.

## O que já é controle, e onde

- Isolamento entre clínicas pelo Postgres (RLS com `FORCE`), provado por teste
  que consulta o catálogo — `docs/architecture/tenancy.md`.
- Nenhum segredo no repositório (gitleaks bloqueante) e nenhum PII em log,
  outbox ou Sentry (`ScrubberDePii`, `LogSemPii`).
- Imagem da API sem root, base pinada por digest, assinada com cosign e com
  atestado de proveniência (`api/Dockerfile`, `.github/workflows/imagem.yml`).
- Cabeçalhos de segurança no web (CSP, HSTS, `frame-ancestors 'none'`),
  guardados por teste (`web/src/borda.test.mjs`).
