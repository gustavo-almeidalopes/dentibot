import TiltCard from './TiltCard.jsx';
import { detectarBandeira, numeroExibido } from '../cartao.js';

/* Marcas desenhadas à mão, de propósito simples: servem para o preview
   reconhecer a bandeira. Em produção, Visa, Mastercard, Elo e Amex exigem o uso
   dos ARQUIVOS OFICIAIS delas, com regras próprias de proporção, área de
   respiro e fundo — está no contrato da adquirente, não é preferência estética.
   Troque estes SVGs pelos oficiais antes de ir ao ar. */
const MARCAS = {
  visa: (
    <svg viewBox="0 0 48 16" className="marca" aria-hidden="true">
      <text x="0" y="13" className="marca-texto" style={{ fontStyle: 'italic', letterSpacing: '.06em' }}>VISA</text>
    </svg>
  ),
  mastercard: (
    <svg viewBox="0 0 48 16" className="marca" aria-hidden="true">
      <circle cx="18" cy="8" r="7.5" fill="currentColor" opacity=".95" />
      <circle cx="29" cy="8" r="7.5" fill="currentColor" opacity=".55" />
    </svg>
  ),
  amex: (
    <svg viewBox="0 0 48 16" className="marca" aria-hidden="true">
      <text x="0" y="12" className="marca-texto" style={{ fontSize: '9px' }}>AMEX</text>
    </svg>
  ),
  elo: (
    <svg viewBox="0 0 48 16" className="marca" aria-hidden="true">
      <text x="0" y="13" className="marca-texto">elo</text>
    </svg>
  ),
  hipercard: (
    <svg viewBox="0 0 48 16" className="marca" aria-hidden="true">
      <text x="0" y="12" className="marca-texto" style={{ fontSize: '8px' }}>HIPER</text>
    </svg>
  ),
  diners: (
    <svg viewBox="0 0 48 16" className="marca" aria-hidden="true">
      <text x="0" y="12" className="marca-texto" style={{ fontSize: '8px' }}>DINERS</text>
    </svg>
  ),
  discover: (
    <svg viewBox="0 0 48 16" className="marca" aria-hidden="true">
      <text x="0" y="12" className="marca-texto" style={{ fontSize: '7px' }}>DISCOVER</text>
    </svg>
  ),
};

/**
 * Cartão de crédito que se preenche enquanto o cliente digita e vira para
 * mostrar o CVV.
 *
 * <p><b>Este componente é só visual e não guarda nada.</b> Ele recebe o que
 * mostrar e mostra. Quem decide se o número real passa por aqui é quem o usa —
 * e a resposta depende do provedor:
 *
 * <ul>
 *   <li><b>Stripe Elements:</b> os campos moram em iframes de outra origem e o
 *       seu JavaScript não lê o número. Passe {@code numero} vazio (o esqueleto
 *       de bolinhas) e alimente só {@code bandeira}, que vem do evento
 *       {@code change} do Element, e {@code nome}, que é campo seu. É o que
 *       mantém a integração em PCI SAQ A.</li>
 *   <li><b>Campos próprios</b> (demonstração, ou SDK que tokeniza no browser a
 *       partir de inputs seus): passe o número e ele aparece dígito a dígito.</li>
 * </ul>
 *
 * @param virado mostra o verso. Ignorado na Amex, cujo código fica na FRENTE —
 *               virar o cartão de um cliente Amex para pedir um número que está
 *               do outro lado é o tipo de detalhe que vira ligação para o
 *               suporte.
 */
export default function CartaoCredito({
  numero = '',
  nome = '',
  validade = '',
  cvv = '',
  bandeira: bandeiraForcada,
  virado = false,
  mascarar = false,
  ...tilt
}) {
  const bandeira = bandeiraForcada
    ? { id: bandeiraForcada, cvv: bandeiraForcada === 'amex' ? 4 : 3 }
    : detectarBandeira(numero);

  const codigoNaFrente = bandeira?.id === 'amex';
  const mostrandoVerso = virado && !codigoNaFrente;
  const casasDoCvv = bandeira?.cvv ?? 3;

  return (
    <TiltCard {...tilt} className="cartao-tilt">
      <div className="cartao" data-virado={mostrandoVerso ? 'sim' : 'nao'}>

        <div className="cartao-face cartao-frente">
          <div className="cartao-topo">
            <span className="cartao-chip" aria-hidden="true" />
            {bandeira && MARCAS[bandeira.id]
              ? <span className="cartao-marca">{MARCAS[bandeira.id]}</span>
              : null}
          </div>

          {/* aria-live: quem usa leitor de tela precisa saber que a bandeira foi
              reconhecida — é a única confirmação de que o número entrou certo. */}
          <p className="cartao-numero" aria-live="polite">
            {numeroExibido(numero, { mascarar })}
          </p>

          <div className="cartao-rodape">
            <div className="cartao-campo">
              <span className="cartao-rotulo">Titular</span>
              <span className="cartao-valor cartao-nome">
                {nome.trim() ? nome.toUpperCase() : 'NOME NO CARTÃO'}
              </span>
            </div>
            <div className="cartao-campo cartao-campo-validade">
              <span className="cartao-rotulo">Validade</span>
              <span className="cartao-valor">{validade || 'MM/AA'}</span>
            </div>
            {codigoNaFrente && (
              <div className="cartao-campo">
                <span className="cartao-rotulo">CVV</span>
                <span className="cartao-valor">{cvv || '••••'}</span>
              </div>
            )}
          </div>
        </div>

        <div className="cartao-face cartao-verso" aria-hidden={!mostrandoVerso}>
          <div className="cartao-tarja" />
          <div className="cartao-assinatura">
            <span className="cartao-painel" />
            <span className="cartao-cvv">{cvv || '•'.repeat(casasDoCvv)}</span>
          </div>
          <p className="cartao-aviso">
            O código de segurança fica no verso, ao lado do campo de assinatura.
          </p>
        </div>

      </div>
    </TiltCard>
  );
}
