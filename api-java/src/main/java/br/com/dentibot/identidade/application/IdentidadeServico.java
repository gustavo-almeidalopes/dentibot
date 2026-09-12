package br.com.dentibot.identidade.application;

import br.com.dentibot.identidade.IdentidadeApi;
import br.com.dentibot.identidade.PessoaResumo;
import br.com.dentibot.identidade.infrastructure.DentistaRepositorio;
import br.com.dentibot.identidade.infrastructure.PessoaRepositorio;
import br.com.dentibot.identidade.infrastructure.UsuarioRepositorio;
import br.com.dentibot.plataforma.contexto.Papel;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentidadeServico implements IdentidadeApi {

    private final PessoaRepositorio pessoas;
    private final UsuarioRepositorio usuarios;
    private final DentistaRepositorio dentistas;
    private final PasswordEncoder encoder;

    public IdentidadeServico(PessoaRepositorio pessoas, UsuarioRepositorio usuarios,
                             DentistaRepositorio dentistas, PasswordEncoder encoder) {
        this.pessoas = pessoas;
        this.usuarios = usuarios;
        this.dentistas = dentistas;
        this.encoder = encoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PessoaResumo> buscarResumos(Collection<Long> idsPessoa) {
        return pessoas.buscarResumos(idsPessoa);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, PessoaResumo> mapaDeResumos(Collection<Long> idsPessoa) {
        return pessoas.buscarResumos(idsPessoa).stream()
                .collect(Collectors.toMap(PessoaResumo::idPessoa, Function.identity()));
    }

    @Override
    @Transactional
    public long criarPessoa(String nomeCompleto, String cpf, String telefoneCelular, String email) {
        return pessoas.inserir(nomeCompleto, cpf, telefoneCelular, email);
    }

    /**
     * {@code REQUIRED} (o padrão): participa da transação de quem chamou. O
     * onboarding precisa que clínica, configurações, pessoa e usuário nasçam
     * juntos ou não nasçam — meia clínica criada, sem ninguém que consiga
     * entrar, é pior que nenhuma.
     */
    @Override
    @Transactional
    public long criarUsuario(String nomeCompleto, String email, String senhaEmClaro, Papel papel) {
        long idPessoa = pessoas.inserir(nomeCompleto, null, null, email);
        return usuarios.inserir(idPessoa, email, encoder.encode(senhaEmClaro), papel);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<Long> dentistaDoUsuario(long idUsuario) {
        return dentistas.idPorUsuario(idUsuario);
    }

    @Override
    @Transactional(readOnly = true)
    public int contarProfissionaisAtivos() {
        return usuarios.contarProfissionaisAtivos();
    }
}
