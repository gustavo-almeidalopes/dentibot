package br.com.dentibot.pacientes.infrastructure;

import br.com.dentibot.pacientes.NovoPaciente;
import tools.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * A ficha de quem ainda não tem clínica.
 *
 * <p>Repositório de escrita apenas, e isso não é uma limitação temporária: a
 * V20 não dá {@code SELECT} para a aplicação nesta tabela. Enquanto o fluxo de
 * vínculo por CPF não existir, não há leitor legítimo — e ficha de saúde sem
 * dono é o tipo de tabela que vaza por um endpoint distraído. Por isso o
 * {@code INSERT} também não usa {@code RETURNING}: ler a coluna de volta exigiria
 * o privilégio que a migração deliberadamente não concedeu.
 *
 * <p>Sem {@code id_clinica} em lugar nenhum: não há tenant, e é o único
 * repositório do sistema em que isso é correto.
 */
@Repository
public class PreCadastroRepositorio {

    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public PreCadastroRepositorio(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** Append-only: reenviar grava outra linha, e o leitor pega a mais recente. */
    public void salvar(String clerkUserId, NovoPaciente ficha) {
        jdbc.sql("""
                        INSERT INTO pacientes.pre_cadastros
                            (clerk_user_id, nome_completo, cpf, rg, data_nascimento,
                             telefone_celular, email, profissao, responsavel_legal,
                             cep, logradouro, numero, complemento, bairro, cidade, uf,
                             anamnese)
                        VALUES (:sub, :nome, :cpf, :rg, :nascimento,
                                :telefone, :email, :profissao, :responsavel,
                                :cep, :logradouro, :numero, :complemento, :bairro, :cidade, :uf,
                                CAST(:anamnese AS JSONB))
                        """)
                .param("sub", clerkUserId)
                .param("nome", ficha.nomeCompleto())
                .param("cpf", ficha.cpf())
                .param("rg", ficha.rg())
                .param("nascimento", ficha.dataNascimento())
                .param("telefone", ficha.telefoneCelular())
                .param("email", ficha.email())
                .param("profissao", ficha.profissao())
                .param("responsavel", ficha.responsavelLegal())
                .param("cep", ficha.cep())
                .param("logradouro", ficha.logradouro())
                .param("numero", ficha.numero())
                .param("complemento", ficha.complemento())
                .param("bairro", ficha.bairro())
                .param("cidade", ficha.cidade())
                .param("uf", ficha.uf())
                .param("anamnese", comoJson(ficha.anamnese()))
                .update();
    }

    /* Jackson 3 não declara exceção checada aqui, então não há try/catch a
       escrever. O null explícito é que importa: sem ele o mapper devolveria a
       string "null", que o JSONB aceita como valor JSON válido — uma anamnese
       que existe e diz nada, em vez de coluna vazia. */
    private String comoJson(Object valor) {
        return valor == null ? null : json.writeValueAsString(valor);
    }
}
