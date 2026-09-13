/**
 * CPF, CNPJ, CEP e celular — máscara e validação, funções puras.
 *
 * <p>Separado do componente pelo mesmo motivo de cartao.js: é a única parte do
 * cadastro que tem regra de verdade, e regra sem teste apodrece. Ver
 * documento.test.mjs.
 *
 * <p>O dígito verificador é conferido no cliente para o usuário saber do erro de
 * digitação enquanto o campo ainda está na frente dele — NUNCA como autorização.
 * O back-end revalida: `PedidoCadastro` e `NovoPaciente` têm o @Pattern, e o
 * CHECK do Postgres tem a última palavra. Validação de cliente é conveniência;
 * quem manda é o servidor.
 */

export const somenteDigitos = (valor) => (valor ?? '').replace(/\D/g, '');

/** Aplica uma máscara posicional: `#` consome um dígito, o resto é literal. */
function mascarar(digitos, molde) {
  let i = 0;
  let saida = '';
  for (const c of molde) {
    if (i >= digitos.length) break;
    if (c === '#') {
      saida += digitos[i];
      i += 1;
    } else {
      saida += c;
    }
  }
  return saida;
}

export const formatarCpf = (v) => mascarar(somenteDigitos(v).slice(0, 11), '###.###.###-##');
export const formatarCnpj = (v) => mascarar(somenteDigitos(v).slice(0, 14), '##.###.###/####-##');
export const formatarCep = (v) => mascarar(somenteDigitos(v).slice(0, 8), '#####-###');

/**
 * Celular ou fixo. O molde muda com o comprimento porque no meio da digitação
 * um número de 10 dígitos não é "um celular incompleto", é um fixo pronto — e
 * travar no molde de 11 põe o parêntese no lugar errado a cada tecla.
 */
export function formatarTelefone(v) {
  const d = somenteDigitos(v).slice(0, 11);
  return mascarar(d, d.length > 10 ? '(##) #####-####' : '(##) ####-####');
}

/**
 * Dígitos verificadores de CPF e CNPJ: mesma conta, pesos diferentes.
 *
 * <p>Resto < 2 vira 0 — é a regra da Receita, não um atalho. Sem ela todo
 * documento cujo resto dá 0 ou 1 seria recusado.
 */
function digitoModulo11(digitos, pesos) {
  const soma = pesos.reduce((acc, peso, i) => acc + Number(digitos[i]) * peso, 0);
  const resto = soma % 11;
  return resto < 2 ? 0 : 11 - resto;
}

/* Todos iguais passam no módulo 11 — 111.111.111-11 fecha a conta certinho. São
   recusados por lista, não por cálculo, porque o cálculo os aprova. */
const todosIguais = (d) => /^(\d)\1+$/.test(d);

export function cpfValido(valor) {
  const d = somenteDigitos(valor);
  if (d.length !== 11 || todosIguais(d)) return false;
  const p1 = [10, 9, 8, 7, 6, 5, 4, 3, 2];
  const p2 = [11, 10, 9, 8, 7, 6, 5, 4, 3, 2];
  return digitoModulo11(d, p1) === Number(d[9])
      && digitoModulo11(d, p2) === Number(d[10]);
}

export function cnpjValido(valor) {
  const d = somenteDigitos(valor);
  if (d.length !== 14 || todosIguais(d)) return false;
  const p1 = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
  const p2 = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
  return digitoModulo11(d, p1) === Number(d[12])
      && digitoModulo11(d, p2) === Number(d[13]);
}

/**
 * Que documento é este? É o que decide o caminho do cadastro: CNPJ vai direto
 * para o painel de admin, CPF pergunta se é dentista autônomo ou paciente.
 *
 * <p>Decide pelo COMPRIMENTO, não pela validade: com 11 dígitos digitados o
 * usuário está preenchendo um CPF, mesmo que o verificador ainda não feche. Um
 * CPF errado tem que dar "CPF inválido", nunca "não sei o que é isto".
 */
export function tipoDeDocumento(valor) {
  const d = somenteDigitos(valor);
  if (d.length === 11) return 'cpf';
  if (d.length === 14) return 'cnpj';
  return null;
}

export const documentoValido = (valor) =>
  tipoDeDocumento(valor) === 'cpf' ? cpfValido(valor)
    : tipoDeDocumento(valor) === 'cnpj' ? cnpjValido(valor)
      : false;

/**
 * Idade em anos completos na data de referência.
 *
 * <p>Serve para exigir o responsável legal de menor de idade — e por isso a
 * conta é de aniversário já feito, não de anos decorridos: quem faz 18 amanhã
 * ainda é menor hoje.
 */
export function idadeEm(dataNascimento, hoje = new Date()) {
  if (!dataNascimento) return null;
  const [ano, mes, dia] = dataNascimento.split('-').map(Number);
  if (!ano || !mes || !dia) return null;
  let idade = hoje.getFullYear() - ano;
  const passouAniversario =
    hoje.getMonth() + 1 > mes || (hoje.getMonth() + 1 === mes && hoje.getDate() >= dia);
  if (!passouAniversario) idade -= 1;
  return idade;
}

export const menorDeIdade = (dataNascimento, hoje) => {
  const idade = idadeEm(dataNascimento, hoje);
  return idade !== null && idade < 18;
};
