# Mapeamento de Segurança — DentiBot

Documento de evidências para os **10 padrões de segurança** aplicados ao DentiBot
(sistema de gestão odontológica multi-tenant — projeto acadêmico UNICID 2026).

Cada padrão lista: **o controle exigido**, **onde foi implementado** (arquivo + função/objeto)
e **qual a abordagem técnica** adotada.

---

## Sumário

| # | Padrão | Arquivo(s) principal(is) |
|---|--------|--------------------------|
| 01 | [Autenticação & Identidade](#padrão-01--autenticação--identidade) | `services/auth-service/routes.py`, `database/db_escrita.sql` |
| 02 | [Autenticação em Dois Fatores](#padrão-02--autenticação-em-dois-fatores-2fa) | `services/auth-service/routes.py`, `database/02-auth-extensions.sql` |
| 03 | [Cookies & Conformidade LGPD](#padrão-03--cookies--conformidade-lgpd) | `services/auth-service/routes.py`, `gateway/app.py`, `src/js/consent.js` |
| 04 | [Proteção de Segredos & Chaves de API](#padrão-04--proteção-de-segredos--chaves-de-api) | `.gitignore`, `.env.example`, `database/db_escrita.sql`, `.github/workflows/ci.yml` |
| 05 | [Arquitetura de Segurança](#padrão-05--arquitetura-de-segurança) | `gateway/app.py`, `services/*/tenant.py`, `services/patient-service/routes.py` |
| 06 | [Proteção na Camada HTTP](#padrão-06--proteção-na-camada-http) | `frontend/nginx.conf`, `gateway/app.py` |
| 07 | [Controle de Acesso & Rate Limiting](#padrão-07--controle-de-acesso--rate-limiting) | `gateway/app.py`, `database/db_escrita.sql` |
| 08 | [Dados em Repouso & em Trânsito](#padrão-08--dados-em-repouso--em-trânsito) | `database/05-tokenization-extensions.sql`, `services/patient-service/routes.py` |
| 09 | [Monitoramento & Auditoria](#padrão-09--monitoramento--auditoria) | `database/04-audit-extensions.sql`, `services/audit-service/routes.py`, `services/audit-service/listener.py` |
| 10 | [DevSecOps & Ciclo de Vida](#padrão-10--devsecops--ciclo-de-vida) | `.github/workflows/ci.yml`, `.github/workflows/dast.yml`, `.github/dependabot.yml` |

---

## Padrão 01 — Autenticação & Identidade

### Controles exigidos
Hashing seguro de senhas, política de senha forte, proteção contra contas comprometidas,
access/refresh tokens com TTL curto, revogação imediata de sessões, prevenção de lockout
por força-bruta e bloqueio progressivo.

### Implementação

| Controle | Arquivo | Função / Objeto | Abordagem |
|----------|---------|-----------------|-----------|
| Hashing de senha | `services/auth-service/routes.py` | `_pepper()` (linha 81) | HMAC-SHA256 com `PASSWORD_PEPPER` (segredo de sistema) aplicado antes do bcrypt via `sp_autenticar_usuario`; resultado < 72 bytes (limite bcrypt) |
| Verificação bcrypt | `database/db_escrita.sql` | `sp_autenticar_usuario` | Stored procedure com `pgcrypto.crypt()` — bcrypt com salt automático; registra falha e aciona lockout em um único passo atômico |
| Política de senha | `services/auth-service/routes.py` | `_password_issues()` (linha 87) | Mínimo 12 caracteres, maiúscula, minúscula, número e símbolo |
| Senha comprometida (HIBP) | `services/auth-service/routes.py` | `_is_pwned()` (linha 102) | k-anonymity: envia apenas os 5 primeiros caracteres do SHA-1; fail-open em indisponibilidade |
| Access token curto | `services/auth-service/routes.py` | `ACCESS_TTL` (linha 59) | JWT HS256, TTL = 15 min, campo `jti` (UUID v4) |
| Refresh token rotativo | `services/auth-service/routes.py` | `REFRESH_TTL` (linha 60) + `refresh()` (linha 511) | TTL = 7 dias; rotação a cada uso; hash SHA-256 armazenado em `sessoes_usuario`; detecção de reuso por comparação de hash |
| Revogação imediata | `services/auth-service/routes.py` | `_revoke_access()` (linha 140) | Escreve `jti` em `tokens_revogados` (PostgreSQL) **e** no Redis (`revoked:<jti>` com TTL = tempo restante do token) |
| Blocklist no gateway | `gateway/app.py` | `_verify_access()` (linha 103) | Consulta Redis antes de aceitar qualquer token; fallback ao PG se Redis indisponível |
| Lockout progressivo | `database/db_escrita.sql` | `controle_bloqueio_usuario` + `sp_registrar_falha_login` | 5 falhas → bloqueio 30 min; `pg_cron` (`desbloquear-contas`) reativa automaticamente |
| Escalonamento de privilégio | `services/auth-service/routes.py` | `register()` (linha 312) | `role` nunca é aceito do corpo da requisição; sempre atribuído o papel de menor privilégio (`cliente`) da clínica |
| Fail-fast em segredos | `services/auth-service/app.py` + `gateway/app.py` | Bloco de inicialização | `JWT_SECRET` e `PASSWORD_PEPPER` ausentes → `RuntimeError` imediato; sem defaults frágeis |

---

## Padrão 02 — Autenticação em Dois Fatores (2FA)

### Controles exigidos
TOTP ou PIN por e-mail/SMS com hash seguro, uso único, limite de tentativas, codes de
recuperação hash-protegidos e segredo TOTP cifrado em repouso.

### Implementação

| Controle | Arquivo | Função / Objeto | Abordagem |
|----------|---------|-----------------|-----------|
| Suporte a TOTP | `services/auth-service/routes.py` | `setup_2fa()` + `verify_2fa()` (linha 443) | `pyotp.TOTP` com URI QR-code; segredo cifrado com `pgp_sym_encrypt` antes de persistir |
| Suporte a PIN e-mail/SMS | `services/auth-service/routes.py` | `send_pin()` + `verify_2fa()` | PIN de 6 dígitos gerado com `secrets.choice`; hash Argon2 armazenado em `codigo_2fa_hash`; expiração em 5 min (`PIN_TTL`) |
| Hash do código 2FA | `services/auth-service/routes.py` | `_ph.hash()` / `_ph.verify()` | Argon2id via `argon2-cffi`; comparação em tempo constante |
| Limite de tentativas 2FA | `services/auth-service/routes.py` | `verify_2fa()` + `MAX_2FA_ATTEMPTS = 5` (linha 62) | Contador em `codigo_2fa_tentativas`; excedido → conta bloqueada via `sp_registrar_falha_login` |
| Códigos de recuperação | `database/02-auth-extensions.sql` | tabela `codigos_recuperacao` | N códigos gerados na ativação do 2FA, exibidos uma vez, armazenados com hash Argon2; consumo atômico (DELETE + verificação) |
| Segredo TOTP em repouso | `database/02-auth-extensions.sql` | coluna `metodo_2fa`, `codigo_2fa_hash` | `pgp_sym_encrypt` com `TOKEN_VAULT_AES_KEY`; nunca retornado após setup |
| Rate-limit no envio de PIN | `gateway/app.py` | `limiter` + `@limiter.limit("20 per minute")` | Flask-Limiter com backend Redis aplicado sobre `auth/send-2fa` |

---

## Padrão 03 — Cookies & Conformidade LGPD

### Controles exigidos
Cookies de sessão com flags de segurança (HttpOnly, Secure, SameSite), proteção CSRF,
consentimento explícito LGPD para cookies analíticos e conformidade com a Lei 13.709/2018.

### Implementação

| Controle | Arquivo | Função / Objeto | Abordagem |
|----------|---------|-----------------|-----------|
| Cookies de sessão seguros | `services/auth-service/routes.py` | `_set_auth_cookies()` (linha 237) | `HttpOnly=True`, `Secure=True`, `SameSite="Strict"`, `Path=/`; não acessíveis via JavaScript |
| Cookie CSRF (não-HttpOnly) | `services/auth-service/routes.py` | `_set_auth_cookies()` | `csrf_token` cookie legível pelo JS para envio no header `X-CSRF-Token` |
| Validação CSRF (double-submit) | `gateway/app.py` | `_csrf_ok()` (linha 134) | `hmac.compare_digest(header, cookie)` em tempo constante; exigido em POST/PUT/DELETE/PATCH originados de cookie |
| Banner de consentimento LGPD | `src/js/consent.js` | `window.dentibotConsent` | Opt-in explícito antes de qualquer cookie analítico; cookie `dentibot_consent` (`SameSite=Lax`, não identificável); hook `onAccept(fn)` para analytics |
| Tabelas LGPD | `database/db_escrita.sql` | `consentimentos_paciente`, `termos_politicas_lgpd`, `solicitacoes_titulares` | Registro de consentimentos com versão do termo, timestamp e forma de captação; workflow de direitos do titular (acesso, retificação, exclusão) |
| Mobile (sem cookies) | `mobile/android/SecureAuth.kt` + `mobile/ios/SecureAuth.swift` | `AuthRepository` / `AuthService` | Bearer token (sem cookie → sem CSRF); armazenamento seguro nativo |

---

## Padrão 04 — Proteção de Segredos & Chaves de API

### Controles exigidos
Ausência de segredos hardcoded, scanning automatizado no CI, rotação periódica de
credenciais e acesso just-in-time (JIT) a recursos sensíveis.

### Implementação

| Controle | Arquivo | Objeto | Abordagem |
|----------|---------|--------|-----------|
| Exclusão de segredos do VCS | `.gitignore` | regras `*.env`, `instance/`, `*.pem`, `*.key`, keystores | Arquivos de credencial e banco local nunca versionados |
| Documentação de variáveis | `.env.example` | — | Lista todas as variáveis exigidas: `JWT_SECRET`, `PASSWORD_PEPPER`, `POSTGRES_*`, `DB_APP_PASSWORD`, `TOKEN_VAULT_AES_KEY`, `REDIS_URL`, `MAIL_*`, `TWILIO_*`, `INTERNAL_SERVICE_SECRET` |
| Fail-fast em ausência | `gateway/app.py` (linhas 29–40), todos os `services/*/app.py` | bloco de inicialização | Qualquer variável obrigatória ausente → `RuntimeError` antes de servir o primeiro request |
| Secret scanning (CI) | `.github/workflows/ci.yml` | job `secret-scan` | `gitleaks/gitleaks-action@v2` com varredura no histórico completo (`fetch-depth: 0`); bloqueante |
| Roles com validade | `database/db_escrita.sql` | `ALTER ROLE dentibot_app VALID UNTIL ...` | `pg_cron` renova via `sp_aprovar_acesso_jit`; expiração automática sem renovação |
| Acesso JIT ao banco | `database/db_escrita.sql` | `solicitacoes_acesso_jit` + `sp_aprovar_acesso_jit` + cron `revogar-jit` | Roles de produção concedidos por janela de tempo; revogados automaticamente |
| Mobile sem segredos | `mobile/android/SecureAuth.kt` | `ApiConfig` (via `BuildConfig`) | `BASE_URL`, `HOST` e `CERT_PIN` injetados pelo CI via `buildConfigField`; ausentes do código-fonte |
| Mobile iOS sem segredos | `mobile/ios/SecureAuth.swift` | `ApiConfig` (via `ProcessInfo.processInfo.environment`) | Valores carregados de variáveis de ambiente de build (xcconfig); nenhum segredo em `Info.plist` |

---

## Padrão 05 — Arquitetura de Segurança

### Controles exigidos
Gateway como único ponto de entrada (BFF/PEP), RBAC/ABAC, isolamento multi-tenant
com RLS, propagação segura de contexto e prevenção de IDOR/BOLA.

### Implementação

| Controle | Arquivo | Função / Objeto | Abordagem |
|----------|---------|-----------------|-----------|
| Gateway como PEP | `gateway/app.py` | `_authenticate()` (linha 141) + `ROUTE_ROLES` (linha 82) | Nenhum serviço expõe porta no host; todo request passa pela verificação de token + papel + RLS antes do proxy |
| RBAC coarse no gateway | `gateway/app.py` | `ROUTE_ROLES` | Mapa serviço → conjunto de papéis; `financeiro` só para `admin/coordenador/financeiro`; `audit` só para `admin/coordenador` |
| Propagação de contexto assinada | `gateway/app.py` | `_internal_headers()` (linha 157) + `_sign_context()` (linha 98) | Headers `X-Internal-*` assinados com HMAC-SHA256 (`INTERNAL_SERVICE_SECRET`); serviços rejeitam contexto com assinatura inválida |
| Anti-spoofing | `gateway/app.py` | `_BLOCKED_CLIENT_HEADERS` (linha 92) | Headers `X-Internal-*` do cliente são removidos antes do proxy; cliente nunca pode forjar contexto de tenant |
| RLS multi-tenant (PG) | `services/*/tenant.py` | `init_tenant_guard()` + `apply_rls()` | Cada `before_request` valida a assinatura HMAC e executa `SET LOCAL app.clinica_id`; políticas `tenant_isolation` isolam dados por clínica no PostgreSQL |
| Anti-IDOR em nível de aplicação | `services/patient-service/routes.py` | `get_patient()` (linha 114) | `Paciente.query.get(pid)` retorna `None` se o RLS filtrar → HTTP 404 (não 403, evitando enumeração) |
| Anti-IDOR + dupla defesa | `services/patient-service/routes.py` | `create_patient()` (linha 71) | `clinic_id` vem exclusivamente do contexto JWT; nunca aceito do corpo da requisição |
| Sanitização de entrada | `services/communication-service/routes.py` | uso de `markupsafe.escape()` | Todos os dados de usuário inseridos em templates HTML são escapados; detalhes de exceção nunca retornam ao cliente (HTTP 502) |
| ORM parametrizado | todos os `services/*/routes.py` | `SQLAlchemy` + `text("... :param")` | Nenhuma query por concatenação de string; prevenção automática de SQL injection |

---

## Padrão 06 — Proteção na Camada HTTP

### Controles exigidos
TLS 1.2+/1.3, HSTS, Content Security Policy, headers de segurança obrigatórios, CORS
restritivo e redirecionamento HTTP→HTTPS.

### Implementação

| Controle | Arquivo | Diretiva / Função | Valor |
|----------|---------|-------------------|-------|
| Redirecionamento HTTP→HTTPS | `frontend/nginx.conf` | `server { listen 80; return 301 ... }` | Todo tráfego HTTP é redirecionado permanentemente |
| TLS 1.2+/1.3 apenas | `frontend/nginx.conf` | `ssl_protocols TLSv1.2 TLSv1.3` (linha 21) | Sem SSLv3/TLS 1.0/1.1 |
| HSTS | `frontend/nginx.conf` | `add_header Strict-Transport-Security` (linha 31) | `max-age=63072000; includeSubDomains; preload` |
| Clickjacking | `frontend/nginx.conf` | `add_header X-Frame-Options "DENY"` (linha 32) | Sem embedding em iframe |
| MIME sniffing | `frontend/nginx.conf` | `add_header X-Content-Type-Options "nosniff"` (linha 33) | Tipo MIME não é adivinhado pelo browser |
| Referrer | `frontend/nginx.conf` | `add_header Referrer-Policy "strict-origin-when-cross-origin"` | Não vaza path em cross-origin |
| Permissions Policy | `frontend/nginx.conf` | `add_header Permissions-Policy` (linha 35) | Câmera, microfone, geolocalização e pagamento desativados |
| Content Security Policy | `frontend/nginx.conf` | `add_header Content-Security-Policy` (linha 36) | `default-src 'self'`; script apenas de `'self'` + Google Accounts; `object-src 'none'`; `frame-ancestors 'none'` |
| CORS restritivo | `gateway/app.py` | `CORS(app, origins=_origins)` (linha 44) | Origem lida de `FRONTEND_ORIGIN` env var; sem wildcard com `credentials: true` |
| Headers internos HMAC | `gateway/app.py` | `_internal_headers()` + `_sign_context()` | Contexto de tenant assinado; serviços validam antes de ativar RLS |
| Pinning de certificado (Android) | `mobile/android/SecureAuth.kt` | `CertificatePinner` (linha 61) | Recusa qualquer certificado não correspondente ao pin SHA-256 injetado pelo CI |
| Pinning de certificado (iOS) | `mobile/ios/SecureAuth.swift` | `PinnedSessionDelegate` + `spkiSHA256()` | SPKI SHA-256 base64; `cancelAuthenticationChallenge` em caso de divergência |

---

## Padrão 07 — Controle de Acesso & Rate Limiting

### Controles exigidos
Rate limiting por IP em endpoints críticos, throttling de força-bruta, proteção CSRF
e controle de acesso baseado em papéis em todo o perímetro.

### Implementação

| Controle | Arquivo | Função / Objeto | Valor / Abordagem |
|----------|---------|-----------------|-------------------|
| Rate limiting global | `gateway/app.py` | `Limiter(default_limits=["200 per minute"])` (linha 60) | Flask-Limiter com backend Redis; 200 req/min por IP |
| Rate limiting em auth | `gateway/app.py` | `@limiter.limit("20 per minute")` em rotas auth | 20 req/min por IP nos endpoints de login, register e 2FA |
| Lockout de conta | `database/db_escrita.sql` | `controle_bloqueio_usuario` + `sp_registrar_falha_login` | 5 falhas → bloqueio 30 min; `pg_cron` desbloqueia automaticamente; atômico e auditado |
| CSRF double-submit (web) | `gateway/app.py` | `_csrf_ok()` (linha 134) | Header `X-CSRF-Token` comparado ao cookie `csrf_token` com `hmac.compare_digest` (tempo constante); exigido em todos os métodos de escrita |
| CSRF (mobile) | — | N/A | Mobile usa Bearer token (sem cookie) → CSRF não se aplica |
| RBAC no gateway | `gateway/app.py` | `_authenticate(allowed=ROUTE_ROLES[svc])` | Papel extraído do JWT; acesso negado com 403 se insuficiente |
| RBAC no nível do banco | `database/db_escrita.sql` | políticas `rls_dentistas_proprios_dados`, `rls_*` | PostgreSQL RLS aplica restrições de papel (e.g., dentista só vê seus próprios pacientes) |

---

## Padrão 08 — Dados em Repouso & em Trânsito

### Controles exigidos
Criptografia de PII em repouso, tokenização de dados sensíveis (CPF), TLS em todo o
tráfego e proteção contra acesso direto ao armazenamento.

### Implementação

| Controle | Arquivo | Função / Objeto | Abordagem |
|----------|---------|-----------------|-----------|
| Tokenização de CPF (escrita) | `services/patient-service/routes.py` | `_tokenize()` (linha 27) | Chama `token_vault.fn_tokenizar(valor, tipo, chave)` no PostgreSQL; retorna token opaco `tok_*` |
| Tokenização de CPF (leitura) | `services/patient-service/routes.py` | `_detokenize()` (linha 37) | Chama `token_vault.fn_detokenizar(token, chave)` apenas em leitura autorizada; tokens inválidos são devolvidos como-estão (passthrough) |
| AES-256 no vault | `database/05-tokenization-extensions.sql` | `token_vault.fn_tokenizar` + `mapa_tokens` | `pgcrypto.pgp_sym_encrypt` com `TOKEN_VAULT_AES_KEY`; mapeamento opaco token↔PII |
| Role dedicado para tokenização | `database/db_escrita.sql` | `dentibot_tokenizer` | Único role com `EXECUTE` em `fn_tokenizar`/`fn_detokenizar`; `dentibot_app` tem apenas a chamada indireta |
| CPF mascarado em listagens | `services/patient-service/routes.py` | `list_patients()` (linha 52) | CPF exibido como `"***"` em listagens paginadas; detokenização apenas em leitura individual autorizada |
| TLS em trânsito | `frontend/nginx.conf` | `ssl_protocols TLSv1.2 TLSv1.3` | Todo tráfego externo cifrado; sem downgrade |
| Headers internos assinados | `gateway/app.py` | `_internal_headers()` | Contexto de tenant não viaja em texto claro; HMAC-SHA256 garante integridade |
| Prontuário imutável | `database/db_escrita.sql` | `trg_imutavel_evolucao` | Trigger `BEFORE UPDATE OR DELETE` aborta qualquer modificação em `evolucao_clinica` |

---

## Padrão 09 — Monitoramento & Auditoria

### Controles exigidos
Log de auditoria imutável (WORM) com encadeamento de hash, alertas em tempo real,
notificações ao usuário em eventos de segurança e detecção de intrusão.

### Implementação

| Controle | Arquivo | Função / Objeto | Abordagem |
|----------|---------|-----------------|-----------|
| Tabela WORM | `database/04-audit-extensions.sql` | `logs_auditoria` + `fn_bloquear_edicao_log()` | Trigger `BEFORE UPDATE OR DELETE` lança exceção; `dentibot_app` tem apenas `SELECT` e `INSERT` |
| Encadeamento de hash | `services/audit-service/routes.py` | `_chain_hash()` (linha 22) | `SHA256(prev_hash | campo1 | ... | campoN)`; qualquer adulteração quebra a cadeia |
| Hash por clínica | `services/audit-service/routes.py` | `create_log()` (linha 35) | Último hash é consultado com RLS ativo → isolamento de cadeia por tenant |
| Bridge pg_notify | `services/audit-service/listener.py` | `run()` (linha 63) | Escuta `auditoria_login`, `auditoria_escrita`, `honeytoken_alert`; persiste com hash; reconecta automaticamente |
| Honeytokens | `database/db_escrita.sql` | `honeytokens` + `fn_detectar_escrita_isca` | Trigger `AFTER INSERT OR UPDATE` em `pacientes` com CPF honeytoken dispara `pg_notify('honeytoken_alert')` |
| RLS no log | `database/04-audit-extensions.sql` | política `tenant_isolation` em `logs_auditoria` | Cada clínica vê apenas seus próprios registros de auditoria |
| Notificações ao usuário | `services/communication-service/routes.py` | rotas de e-mail/SMS | Disparadas pelo auth-service em: novo login, troca de credenciais, dispositivo novo |

---

## Padrão 10 — DevSecOps & Ciclo de Vida

### Controles exigidos
Pipeline CI bloqueante com secret scanning, análise de composição de software (SCA),
SAST, geração de SBOM, DAST automatizado e atualização contínua de dependências.

### Implementação

| Controle | Arquivo | Job / Ferramenta | Configuração |
|----------|---------|------------------|-------------|
| Secret scanning | `.github/workflows/ci.yml` | job `secret-scan` → `gitleaks/gitleaks-action@v2` | Varredura no histórico completo (`fetch-depth: 0`); bloqueante no PR |
| SCA (dependências) | `.github/workflows/ci.yml` | job `sca` → `pip-audit --strict` | Percorre todos os `requirements.txt`; qualquer CVE conhecido reprova o build |
| SBOM | `.github/workflows/ci.yml` | job `sca` → `cyclonedx-bom` | Gera `sbom-*.json` por serviço; publicado como artefato GitHub Actions |
| SAST — bandit | `.github/workflows/ci.yml` | job `sast` → `bandit -r gateway services -lll` | Bloqueante em alta severidade (`-lll`); cobre gateway e todos os serviços |
| SAST — semgrep | `.github/workflows/ci.yml` | job `sast` → `returntocorp/semgrep-action@v1` | Rulesets: `p/python`, `p/flask`, `p/security-audit`, `p/secrets` |
| DAST | `.github/workflows/dast.yml` | `zaproxy/action-baseline@v0.12.0` | Scan semanal (seg 03:00 UTC) + `workflow_dispatch` contra ambiente de homolog; `fail_action: true` |
| Dependabot (pip) | `.github/dependabot.yml` | 8 entradas `pip` | PRs automáticos semanais para `gateway` e cada um dos 7 microserviços |
| Dependabot (docker) | `.github/dependabot.yml` | 2 entradas `docker` | Imagens base de `docker/postgres` e `frontend` |
| Dependabot (actions) | `.github/dependabot.yml` | 1 entrada `github-actions` | Actions usadas nos workflows de CI/CD |
| Acesso JIT ao banco | `database/db_escrita.sql` | `sp_aprovar_acesso_jit` + cron `revogar-jit` | Roles de produção expiram automaticamente; renovação exige aprovação explícita |
| Pinning mobile (Android) | `mobile/android/SecureAuth.kt` | `CertificatePinner` | Pin SHA-256 injetado via `BuildConfig` pelo CI; não versionado |
| Pinning mobile (iOS) | `mobile/ios/SecureAuth.swift` | `PinnedSessionDelegate` | SPKI SHA-256 lido de variável de build; ATS força TLS 1.2+ |

---

## Referência de arquivos por domínio

```
database/
  db_escrita.sql              ← schema canônico: RLS, stored procedures, honeytokens
  02-auth-extensions.sql      ← tabelas de sessão, 2FA, tokens_revogados, códigos recuperação
  03-domain-extensions.sql    ← RLS para tabelas sem id_clinica, estoque
  04-audit-extensions.sql     ← logs_auditoria WORM, trigger fn_bloquear_edicao_log
  05-tokenization-extensions.sql ← token_vault.fn_detokenizar, grants

gateway/
  app.py                      ← PEP: RBAC, rate limiting, CSRF, propagação de tenant

services/
  auth-service/
    app.py                    ← fail-fast secrets, CORS restritivo
    routes.py                 ← autenticação, 2FA, refresh rotation, lockout
    models.py                 ← mapeamento canonical PG
    tenant.py                 ← validação HMAC + apply_rls
  patient-service/
    routes.py                 ← tokenização CPF, anti-IDOR, RLS
    tenant.py
  audit-service/
    routes.py                 ← log WORM, hash-chaining
    listener.py               ← bridge pg_notify → logs_auditoria
  communication-service/
    routes.py                 ← escape de HTML, sem vazar exceções

frontend/
  nginx.conf                  ← TLS 1.2+/1.3, HSTS, CSP, security headers
  Dockerfile                  ← geração de certificado self-signed

src/js/
  api.js                      ← fetch com credentials:include, CSRF header, refresh automático
  login.js                    ← login real (sem backdoor), Google Sign-In server-side
  consent.js                  ← banner LGPD opt-in

mobile/
  android/SecureAuth.kt       ← EncryptedSharedPreferences, OkHttp CertificatePinner
  ios/SecureAuth.swift        ← Keychain WhenUnlockedThisDeviceOnly, SPKI pinning

.github/
  workflows/ci.yml            ← gitleaks, pip-audit, bandit -lll, semgrep, SBOM
  workflows/dast.yml          ← OWASP ZAP baseline semanal
  dependabot.yml              ← PRs automáticos: pip × 8, docker × 2, github-actions

.gitignore                    ← exclui .env, instance/, *.pem, *.key, keystores
.env.example                  ← documenta todas as variáveis obrigatórias
```

---

*Gerado em 2026-06-08. Para dúvidas sobre a implementação, consulte os comentários
de cabeçalho em cada arquivo listado acima.*
