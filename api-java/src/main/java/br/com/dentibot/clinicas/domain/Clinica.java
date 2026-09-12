package br.com.dentibot.clinicas.domain;

import java.time.Instant;
import java.time.ZoneId;

public record Clinica(
        long id,
        String cnpj,
        String razaoSocial,
        String nomeFantasia,
        ZoneId timezone,
        Plano plano,
        StatusClinica status,
        Instant criadaEm) {

    /**
     * O fuso é da clínica, não do servidor nem do navegador. Toda renderização de
     * horário de agenda passa por aqui; o armazenamento continua em UTC
     * (TIMESTAMPTZ), invariante 7.
     */
    public ZoneId fusoParaExibicao() {
        return timezone;
    }
}
