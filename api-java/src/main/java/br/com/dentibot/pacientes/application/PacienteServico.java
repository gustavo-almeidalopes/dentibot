package br.com.dentibot.pacientes.application;

import br.com.dentibot.identidade.DadosPessoais;
import br.com.dentibot.identidade.IdentidadeApi;
import br.com.dentibot.identidade.PessoaResumo;
import br.com.dentibot.pacientes.NovoPaciente;
import br.com.dentibot.pacientes.PacienteResumo;
import br.com.dentibot.pacientes.PacientesApi;
import br.com.dentibot.pacientes.infrastructure.PacienteRepositorio;
import br.com.dentibot.pacientes.infrastructure.PacienteRepositorio.LinhaPaciente;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PacienteServico implements PacientesApi {

    private final PacienteRepositorio pacientes;
    private final IdentidadeApi identidade;

    public PacienteServico(PacienteRepositorio pacientes, IdentidadeApi identidade) {
        this.pacientes = pacientes;
        this.identidade = identidade;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PacienteResumo> listarResumos(int limite, long apos) {
        List<LinhaPaciente> linhas = pacientes.listar(limite, apos);
        return montar(linhas);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, PacienteResumo> mapaDeResumos(Collection<Long> idsPaciente) {
        return montar(pacientes.buscarPorIds(List.copyOf(idsPaciente))).stream()
                .collect(Collectors.toMap(PacienteResumo::idPaciente, Function.identity()));
    }

    /**
     * Duas consultas em vez de um JOIN: uma no schema de pacientes, outra na
     * porta de identidade. É de propósito. O JOIN entre schemas de módulos
     * diferentes funcionaria hoje e é exatamente o que impediria extrair
     * qualquer um dos dois depois — e some em silêncio na revisão, porque parece
     * apenas uma consulta eficiente.
     *
     * <p>O custo é uma ida a mais ao banco por listagem, com os ids já em mãos.
     */
    private List<PacienteResumo> montar(List<LinhaPaciente> linhas) {
        if (linhas.isEmpty()) {
            return List.of();
        }
        Map<Long, PessoaResumo> pessoas = identidade.mapaDeResumos(
                linhas.stream().map(LinhaPaciente::idPessoa).toList());

        return linhas.stream()
                .map(l -> {
                    PessoaResumo p = pessoas.get(l.idPessoa());
                    return new PacienteResumo(
                            l.idPaciente(),
                            l.idPessoa(),
                            p == null ? null : p.nomeCompleto(),
                            p == null ? null : p.telefoneCelular(),
                            l.status());
                })
                .toList();
    }

    @Override
    @Transactional
    public long criar(NovoPaciente novo) {
        long idPessoa = identidade.criarPessoa(new DadosPessoais(
                novo.nomeCompleto(), novo.cpf(), novo.rg(), novo.dataNascimento(),
                novo.telefoneCelular(), novo.email(), novo.profissao(), novo.responsavelLegal(),
                novo.cep(), novo.logradouro(), novo.numero(), novo.complemento(),
                novo.bairro(), novo.cidade(), novo.uf()));
        return pacientes.inserir(idPessoa, novo.idPlanoConvenio(), novo.numeroCarteirinha(),
                novo.anamnese());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existe(long idPaciente) {
        return pacientes.existe(idPaciente);
    }
}
