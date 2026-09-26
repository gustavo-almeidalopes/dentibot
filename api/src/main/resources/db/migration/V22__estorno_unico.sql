-- ─────────────────────────────────────────────────────────────────────────────
-- V22 — Um lançamento se estorna uma vez só.
--
-- FinanceiroServico.estornar() consulta jaEstornado() e depois insere. Em READ
-- COMMITTED, dois cliques simultâneos não enxergam o estorno um do outro, os
-- dois passam pela checagem e o valor volta em dobro para o paciente. A
-- checagem continua lá para dar mensagem clara no caso sequencial; quem
-- garante é o índice. O segundo INSERT espera o commit do primeiro e recebe
-- 23505, que o TratadorGlobalDeErros responde como 409.
--
-- Sem id_clinica na chave: id_lancamento é identidade global, e um lançamento
-- tem um estorno no máximo, em qualquer clínica.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE UNIQUE INDEX uq_lancamento_estorno_unico
    ON financeiro.lancamentos (estorna_lancamento)
    WHERE estorna_lancamento IS NOT NULL;
