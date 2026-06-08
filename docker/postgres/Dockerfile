# PostgreSQL 16 com as extensões exigidas por database/db_escrita.sql:
#   - pgcrypto  (já vem no contrib) — crypt()/bcrypt, pgp_sym_encrypt (AES-256), digest()
#   - pgaudit   — auditoria a nível de banco (Padrão 09)
#   - pg_cron   — jobs agendados: desbloqueio de contas, revogação de acesso JIT
FROM postgres:18

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        postgresql-16-cron \
        postgresql-16-pgaudit \
    && rm -rf /var/lib/apt/lists/*

# pg_cron e pgaudit precisam ser carregados via shared_preload_libraries.
# O valor efetivo é passado pelo `command:` no docker-compose (inclui também
# cron.database_name apontando para o banco da aplicação).
