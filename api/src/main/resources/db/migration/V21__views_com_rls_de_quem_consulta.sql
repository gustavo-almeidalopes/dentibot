-- ─────────────────────────────────────────────────────────────────────────────
-- V21 — As views passam a rodar com os privilégios de quem consulta.
--
-- Sem security_invoker, uma view roda como o DONO dela — o dentibot_migrador.
-- As políticas de RLS só nomeiam dentibot_app, e as tabelas têm FORCE ROW LEVEL
-- SECURITY, que vale também para o dono. Resultado: o migrador enxerga zero
-- linhas, e a view devolve vazio para todo mundo, sem erro nenhum.
--
-- Na prática, desde a V14 nenhum recebível era encontrado: o repositório faz
-- JOIN com vw_saldo_recebivel, e registrar recebimento ou estornar respondia
-- 404 para um recebível que existia. A posição de estoque da V15, idem.
--
-- Com security_invoker a view é avaliada como dentibot_app, sob a política
-- tenant_isolation das tabelas de baixo — que é o isolamento que se queria.
-- ─────────────────────────────────────────────────────────────────────────────

ALTER VIEW financeiro.vw_saldo_recebivel SET (security_invoker = true);
ALTER VIEW estoque.vw_posicao            SET (security_invoker = true);
