import { useState } from 'react';
import {
  cpfValido, formatarCep, formatarCpf, formatarTelefone, menorDeIdade, somenteDigitos,
} from '../documento.js';

/**
 * Ficha do paciente: identificação, endereço e a triagem de saúde.
 *
 * <p>Um componente só, dois chamadores — a recepção em /pacientes e o
 * auto-cadastro em /cadastro. Eram duas telas candidatas a divergir campo a
 * campo, e a que ficasse para trás perderia justamente a anamnese, que é o
 * pedaço que muda a conduta clínica.
 *
 * <p>O que este componente NÃO faz: decidir para onde os dados vão. Ele monta o
 * corpo e entrega em `onEnviar`. Quem sabe o endpoint é a tela.
 */

const UFS = ('AC AL AP AM BA CE DF ES GO MA MT MS MG PA PB PR PE PI RJ RN RS RO RR SC SP SE TO')
  .split(' ');

const MOTIVOS = [
  ['dor', 'Dor'],
  ['estetica', 'Estética'],
  ['limpeza', 'Limpeza'],
  ['rotina', 'Rotina'],
];

const VAZIO = {
  nomeCompleto: '', dataNascimento: '', cpf: '', rg: '', telefoneCelular: '',
  email: '', profissao: '', responsavelLegal: '',
  cep: '', logradouro: '', numero: '', complemento: '', bairro: '', cidade: '', uf: '',
  emTratamentoMedico: '', medicamentoContinuo: '', alergia: '',
  condicaoSistemica: '', gravidez: 'nao_se_aplica', motivoConsulta: '',
  sensibilidade: '', sangramentoGengival: '',
};

/** Sim/Não como <select>: é nativo, navega por teclado e não precisa de fieldset. */
function SimNao({ rotulo, valor, aoMudar, obrigatorio = false, extra = null }) {
  return (
    <>
      <label className="campo-app">
        <span className="cap cap-ash">{rotulo}</span>
        <select required={obrigatorio} value={valor} onChange={aoMudar}>
          <option value="">—</option>
          <option value="sim">Sim</option>
          <option value="nao">Não</option>
        </select>
      </label>
      {valor === 'sim' && extra}
    </>
  );
}

export default function FichaPaciente({
  onEnviar, enviando, erro, inicial, rotuloEnviar = 'Cadastrar', children,
}) {
  const [f, setF] = useState({ ...VAZIO, ...inicial });

  const mudar = (campo, formatar) => (e) =>
    setF((atual) => ({ ...atual, [campo]: formatar ? formatar(e.target.value) : e.target.value }));

  /* O responsável vira obrigatório sozinho, a partir da data de nascimento —
     não por um checkbox que o recepcionista possa esquecer de marcar. */
  const exigeResponsavel = menorDeIdade(f.dataNascimento);
  const cpfErrado = f.cpf !== '' && !cpfValido(f.cpf);

  function enviar(e) {
    e.preventDefault();
    if (cpfErrado) return;
    /* Vazio vira null e não '': o CHECK de CPF do banco recusa string vazia, e
       e-mail '' ocuparia o índice único à toa. */
    const texto = (v) => (v.trim() === '' ? null : v.trim());
    const booleano = (v) => (v === '' ? null : v === 'sim');

    onEnviar({
      nomeCompleto: f.nomeCompleto.trim(),
      dataNascimento: f.dataNascimento || null,
      cpf: somenteDigitos(f.cpf) || null,
      rg: texto(f.rg),
      telefoneCelular: somenteDigitos(f.telefoneCelular) || null,
      email: texto(f.email),
      profissao: texto(f.profissao),
      responsavelLegal: texto(f.responsavelLegal),
      cep: somenteDigitos(f.cep) || null,
      logradouro: texto(f.logradouro),
      numero: texto(f.numero),
      complemento: texto(f.complemento),
      bairro: texto(f.bairro),
      cidade: texto(f.cidade),
      uf: texto(f.uf),
      anamnese: {
        emTratamentoMedico: booleano(f.emTratamentoMedico),
        medicamentoContinuo: texto(f.medicamentoContinuo),
        alergia: texto(f.alergia),
        condicaoSistemica: booleano(f.condicaoSistemica),
        gravidez: f.gravidez === 'nao_se_aplica' ? null : f.gravidez === 'sim',
        motivoConsulta: f.motivoConsulta || null,
        sensibilidade: booleano(f.sensibilidade),
        sangramentoGengival: booleano(f.sangramentoGengival),
      },
    });
  }

  return (
    <form className="form-bloco" onSubmit={enviar}>
      <h2 className="sub">1 · Dados pessoais e contato</h2>

      <div className="form-linha">
        <label className="campo-app" style={{ flex: '2 1 320px' }}>
          <span className="cap cap-ash">Nome completo *</span>
          <input required maxLength={150} autoComplete="name"
                 value={f.nomeCompleto} onChange={mudar('nomeCompleto')} />
        </label>
        <label className="campo-app">
          <span className="cap cap-ash">Data de nascimento *</span>
          {/* type="date" e não um datepicker: o nativo já traz calendário,
              teclado, locale e leitor de tela. A data é o que decide dose de
              anestésico e conduta infantil, então ela é obrigatória. */}
          <input required type="date" max={new Date().toISOString().slice(0, 10)}
                 value={f.dataNascimento} onChange={mudar('dataNascimento')} />
        </label>
      </div>

      <div className="form-linha">
        <label className="campo-app">
          <span className="cap cap-ash">CPF</span>
          <input inputMode="numeric" maxLength={14} placeholder="000.000.000-00"
                 aria-invalid={cpfErrado || undefined}
                 value={f.cpf} onChange={mudar('cpf', formatarCpf)} />
        </label>
        <label className="campo-app">
          <span className="cap cap-ash">RG</span>
          <input maxLength={20} value={f.rg} onChange={mudar('rg')} />
        </label>
        <label className="campo-app">
          <span className="cap cap-ash">Celular / WhatsApp *</span>
          <input required type="tel" inputMode="numeric" maxLength={15} autoComplete="tel"
                 placeholder="(11) 98765-4321"
                 value={f.telefoneCelular} onChange={mudar('telefoneCelular', formatarTelefone)} />
        </label>
      </div>

      {cpfErrado && (
        <p className="pagamento-erro" role="alert">
          CPF inválido — confira os dígitos.
        </p>
      )}

      <div className="form-linha">
        <label className="campo-app">
          <span className="cap cap-ash">E-mail</span>
          <input type="email" maxLength={254} autoComplete="email"
                 value={f.email} onChange={mudar('email')} />
        </label>
        <label className="campo-app">
          <span className="cap cap-ash">Profissão</span>
          <input maxLength={80} value={f.profissao} onChange={mudar('profissao')} />
        </label>
      </div>

      {exigeResponsavel && (
        <label className="campo-app">
          <span className="cap cap-ash">Responsável legal * (paciente menor de idade)</span>
          <input required maxLength={150}
                 value={f.responsavelLegal} onChange={mudar('responsavelLegal')} />
        </label>
      )}

      <h2 className="sub">2 · Endereço</h2>

      <div className="form-linha">
        <label className="campo-app" style={{ flex: '0 1 140px' }}>
          <span className="cap cap-ash">CEP</span>
          <input inputMode="numeric" maxLength={9} autoComplete="postal-code"
                 placeholder="00000-000" value={f.cep} onChange={mudar('cep', formatarCep)} />
        </label>
        <label className="campo-app" style={{ flex: '3 1 280px' }}>
          <span className="cap cap-ash">Rua</span>
          <input maxLength={255} autoComplete="address-line1"
                 value={f.logradouro} onChange={mudar('logradouro')} />
        </label>
        <label className="campo-app" style={{ flex: '0 1 100px' }}>
          <span className="cap cap-ash">Número</span>
          <input maxLength={20} value={f.numero} onChange={mudar('numero')} />
        </label>
      </div>

      <div className="form-linha">
        <label className="campo-app">
          <span className="cap cap-ash">Complemento</span>
          <input maxLength={100} value={f.complemento} onChange={mudar('complemento')} />
        </label>
        <label className="campo-app">
          <span className="cap cap-ash">Bairro</span>
          <input maxLength={100} value={f.bairro} onChange={mudar('bairro')} />
        </label>
        <label className="campo-app">
          <span className="cap cap-ash">Cidade</span>
          <input maxLength={100} autoComplete="address-level2"
                 value={f.cidade} onChange={mudar('cidade')} />
        </label>
        <label className="campo-app" style={{ flex: '0 1 90px' }}>
          <span className="cap cap-ash">Estado</span>
          <select value={f.uf} onChange={mudar('uf')}>
            <option value="">—</option>
            {UFS.map((uf) => <option key={uf} value={uf}>{uf}</option>)}
          </select>
        </label>
      </div>

      <h2 className="sub">3 · Triagem de saúde</h2>
      <p className="cap cap-ash">
        Dado de saúde é sensível (LGPD art. 11) e só a equipe clínica lê.
      </p>

      <div className="form-linha">
        <SimNao
          rotulo="Está em tratamento médico atualmente?"
          valor={f.emTratamentoMedico}
          aoMudar={mudar('emTratamentoMedico')}
        />
        <SimNao
          rotulo="Problemas cardíacos, diabetes ou hipertensão?"
          valor={f.condicaoSistemica}
          aoMudar={mudar('condicaoSistemica')}
        />
      </div>

      <div className="form-linha">
        <label className="campo-app">
          <span className="cap cap-ash">Medicamento de uso contínuo — qual?</span>
          <input maxLength={255} placeholder="deixe em branco se não usa"
                 value={f.medicamentoContinuo} onChange={mudar('medicamentoContinuo')} />
        </label>
        {/* Alergia é campo aberto, e não Sim/Não: "sim" sem o nome do alérgeno
            não muda conduta nenhuma — o que importa é ler "penicilina" ou
            "látex" antes de escolher o material. */}
        <label className="campo-app">
          <span className="cap cap-ash">Alergia a medicamento ou látex — qual?</span>
          <input maxLength={255} placeholder="ex.: penicilina, látex"
                 value={f.alergia} onChange={mudar('alergia')} />
        </label>
      </div>

      <div className="form-linha">
        <label className="campo-app">
          <span className="cap cap-ash">Está grávida?</span>
          <select value={f.gravidez} onChange={mudar('gravidez')}>
            <option value="nao_se_aplica">Não se aplica</option>
            <option value="sim">Sim</option>
            <option value="nao">Não</option>
          </select>
        </label>
        <label className="campo-app">
          <span className="cap cap-ash">Motivo principal da consulta</span>
          <select value={f.motivoConsulta} onChange={mudar('motivoConsulta')}>
            <option value="">—</option>
            {MOTIVOS.map(([v, r]) => <option key={v} value={v}>{r}</option>)}
          </select>
        </label>
      </div>

      <div className="form-linha">
        <SimNao
          rotulo="Sente sensibilidade (frio, calor, doces)?"
          valor={f.sensibilidade}
          aoMudar={mudar('sensibilidade')}
        />
        <SimNao
          rotulo="Sangra ao escovar ou passar fio dental?"
          valor={f.sangramentoGengival}
          aoMudar={mudar('sangramentoGengival')}
        />
      </div>

      {children}

      {erro && <p className="pagamento-erro" role="alert">{erro.message}</p>}

      <button type="submit" className="btn btn-fill btn-lg" disabled={enviando}>
        {enviando ? 'Enviando…' : rotuloEnviar}
      </button>
    </form>
  );
}
