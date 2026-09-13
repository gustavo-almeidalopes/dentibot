import { useState } from 'react';
import CartaoCredito from './CartaoCredito.jsx';
import {
  cartaoCompleto, detectarBandeira, formatarNumero, formatarValidade,
  luhnValido, maximoDeDigitos, somenteDigitos, validadeNoFuturo,
} from '../cartao.js';

/**
 * Formulário de cartão com preview ao vivo.
 *
 * <p><b>Leia isto antes de usar em produção com Stripe.</b> Este componente tem
 * um {@code <input>} próprio para o número do cartão. Com Stripe, isso é a
 * diferença entre PCI SAQ A (umas poucas dezenas de perguntas, porque o número
 * nunca entra no seu domínio) e SAQ D (auditoria anual, varredura trimestral,
 * mais de trezentos controles). A regra do Stripe é literal: os campos precisam
 * ser os Elements, que renderizam em iframes de outra origem.
 *
 * <p>Então o uso legítimo daqui é um destes:
 *
 * <ol>
 *   <li>demonstração e desenvolvimento, com os números de teste;</li>
 *   <li>um provedor cujo SDK tokeniza no browser a partir de campos seus;</li>
 *   <li>como referência visual, trocando os três inputs sensíveis pelos
 *       Elements e mantendo só o nome — ver {@code exemploComStripe} no fim
 *       deste arquivo.</li>
 * </ol>
 *
 * <p>Nada é enviado a lugar nenhum aqui: {@code onEnviar} recebe o que você
 * decidir fazer com isso, e o certo é tokenizar antes de sair do browser.
 */
export default function FormularioCartao({ onEnviar, textoBotao = 'Pagar' }) {
  const [dados, setDados] = useState({ numero: '', nome: '', validade: '', cvv: '' });
  const [virado, setVirado] = useState(false);
  const [tocado, setTocado] = useState({});

  const bandeira = detectarBandeira(dados.numero);
  const casasDoCvv = bandeira?.cvv ?? 3;
  const pronto = cartaoCompleto(dados);

  const mudar = (campo) => (e) => {
    const bruto = e.target.value;
    const valor =
      campo === 'numero' ? formatarNumero(bruto)
      : campo === 'validade' ? formatarValidade(bruto)
      : campo === 'cvv' ? somenteDigitos(bruto).slice(0, casasDoCvv)
      : bruto;
    setDados((d) => ({ ...d, [campo]: valor }));
  };

  const sair = (campo) => () => setTocado((t) => ({ ...t, [campo]: true }));

  /* Erro só depois de o campo ter sido visitado: acusar "número inválido" no
     terceiro dígito é acusar alguém de estar digitando. */
  const erro = (campo) => {
    if (!tocado[campo]) return null;
    if (campo === 'numero') {
      if (!somenteDigitos(dados.numero)) return 'Informe o número do cartão.';
      if (!luhnValido(dados.numero)) return 'Confira o número: algum dígito está trocado.';
      if (!bandeira?.digitos.includes(somenteDigitos(dados.numero).length)) {
        return 'O número está incompleto.';
      }
    }
    if (campo === 'validade' && !validadeNoFuturo(dados.validade)) {
      return 'Validade vencida ou inválida.';
    }
    if (campo === 'cvv' && somenteDigitos(dados.cvv).length !== casasDoCvv) {
      return `O código tem ${casasDoCvv} dígitos neste cartão.`;
    }
    if (campo === 'nome' && dados.nome.trim().length < 2) {
      return 'Informe o nome como está no cartão.';
    }
    return null;
  };

  return (
    <div className="pagamento">
      <CartaoCredito
        numero={dados.numero}
        nome={dados.nome}
        validade={dados.validade}
        cvv={dados.cvv}
        virado={virado}
        tiltLimit={9}
        perspective={900}
        scale={1.02}
      />

      <form
        className="pagamento-campos"
        onSubmit={(e) => {
          e.preventDefault();
          if (pronto) onEnviar?.(dados);
        }}
      >
        <Campo id="cartao-numero" rotulo="Número do cartão" erro={erro('numero')}>
          <input
            id="cartao-numero"
            value={dados.numero}
            onChange={mudar('numero')}
            onBlur={sair('numero')}
            /* inputMode numeric abre o teclado de dígitos no celular;
               autoComplete cc-number deixa o gerenciador de senhas preencher,
               que é mais seguro do que a pessoa digitar de um cartão na mão. */
            inputMode="numeric"
            autoComplete="cc-number"
            placeholder="0000 0000 0000 0000"
            maxLength={maximoDeDigitos(dados.numero) + 4}
            aria-describedby={erro('numero') ? 'erro-numero' : undefined}
          />
        </Campo>

        <Campo id="cartao-nome" rotulo="Nome como está no cartão" erro={erro('nome')}>
          <input
            id="cartao-nome"
            value={dados.nome}
            onChange={mudar('nome')}
            onBlur={sair('nome')}
            autoComplete="cc-name"
            placeholder="ANA MELLO"
          />
        </Campo>

        <div className="pagamento-linha">
          <Campo id="cartao-validade" rotulo="Validade" erro={erro('validade')}>
            <input
              id="cartao-validade"
              value={dados.validade}
              onChange={mudar('validade')}
              onBlur={sair('validade')}
              inputMode="numeric"
              autoComplete="cc-exp"
              placeholder="MM/AA"
              maxLength={5}
            />
          </Campo>

          <Campo id="cartao-cvv" rotulo={bandeira?.id === 'amex' ? 'CVV (4 dígitos)' : 'CVV'}
                 erro={erro('cvv')}>
            <input
              id="cartao-cvv"
              value={dados.cvv}
              onChange={mudar('cvv')}
              /* O giro é do FOCO, não do preenchimento: vira quando a pessoa
                 chega no campo, porque é aí que ela precisa achar o número no
                 cartão físico. */
              onFocus={() => setVirado(true)}
              onBlur={() => { setVirado(false); sair('cvv')(); }}
              inputMode="numeric"
              autoComplete="cc-csc"
              placeholder={'•'.repeat(casasDoCvv)}
              maxLength={casasDoCvv}
            />
          </Campo>
        </div>

        <button type="submit" className="btn btn-fill btn-lg" disabled={!pronto}>
          {textoBotao}
        </button>
      </form>
    </div>
  );
}

function Campo({ id, rotulo, erro, children }) {
  return (
    <div className="pagamento-campo">
      <label htmlFor={id} className="cap cap-ash">{rotulo}</label>
      {children}
      {/* role=alert faz o leitor de tela anunciar na hora, sem esperar foco. */}
      {erro && <p id={`erro-${id}`} className="pagamento-erro" role="alert">{erro}</p>}
    </div>
  );
}

/* ───────────────────────────────────────────────────────────────────────────
   O MESMO CARTÃO, COM STRIPE ELEMENTS

   O preview continua igual; o que muda é de onde vêm os dados. O número e a
   validade deixam de existir do lado de cá — o Element só conta a bandeira e se
   está completo:

     const elements = stripe.elements();
     const numero = elements.create('cardNumber');
     const cvc    = elements.create('cardCvc');
     numero.mount('#campo-numero');

     numero.on('change', (e) => setBandeira(e.brand === 'unknown' ? null : e.brand));
     cvc.on('focus', () => setVirado(true));
     cvc.on('blur',  () => setVirado(false));

     <CartaoCredito
        numero=""                   // esqueleto de bolinhas: você não tem os dígitos
        bandeira={bandeira}         // isto o Element entrega
        nome={nome}                 // campo seu, pode espelhar à vontade
        validade=""                 // idem número
        virado={virado}
     />

   A pergunta que sempre aparece aqui é "dá para mostrar os dígitos aparecendo
   mesmo assim?". Não dá, e não é uma limitação a contornar: se o seu JS
   conseguisse ler aquele campo, qualquer script de terceiro na sua página
   também conseguiria — que é precisamente o ataque que o iframe impede.
   ─────────────────────────────────────────────────────────────────────────── */
