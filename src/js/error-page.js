(function () {
  var code = window.DENTIBOT_ERROR || 500;

  var ERRORS = {
    400: { name: 'Requisição Inválida',                  tagline: 'O raio-X chegou torto!',                      desc: 'O servidor não conseguiu entender a solicitação devido a sintaxe ou roteamento incorretos.',         accent: '#FFE600', icon: '📋' },
    401: { name: 'Não Autorizado',                       tagline: 'Identificação necessária na recepção!',        desc: 'Você precisa se autenticar para acessar este recurso.',                                             accent: '#FFE600', icon: '🔑' },
    402: { name: 'Pagamento Necessário',                  tagline: 'Consulta particular, doc!',                   desc: 'Este recurso requer pagamento para ser acessado.',                                                  accent: '#FFE600', icon: '💳' },
    403: { name: 'Acesso Proibido',                       tagline: 'Área restrita — somente autorizados!',        desc: 'Você não tem permissão para acessar esta área do sistema.',                                         accent: '#FF8C00', icon: '🚫' },
    404: { name: 'Página Não Encontrada',                 tagline: 'Este dente foi extraído!',                    desc: 'A página que você procura não existe ou foi movida para outro endereço.',                           accent: '#00E676', icon: '🦷' },
    405: { name: 'Método Não Permitido',                  tagline: 'Instrumento cirúrgico errado!',               desc: 'O método HTTP usado não é permitido para este recurso.',                                            accent: '#FFE600', icon: '🔧' },
    406: { name: 'Não Aceitável',                         tagline: 'Material incompatível com o tratamento!',     desc: 'O servidor não consegue gerar conteúdo no formato solicitado.',                                     accent: '#FFE600', icon: '❌' },
    407: { name: 'Autenticação de Proxy Necessária',      tagline: 'Passe pela recepção primeiro!',               desc: 'Autentique-se com o servidor proxy antes de continuar.',                                           accent: '#FFE600', icon: '🛡️' },
    408: { name: 'Timeout da Requisição',                 tagline: 'O paciente foi embora esperando!',            desc: 'O servidor não recebeu a solicitação completa dentro do tempo esperado.',                          accent: '#FFE600', icon: '⏰' },
    409: { name: 'Conflito',                              tagline: 'Dois pacientes no mesmo horário!',            desc: 'A requisição conflita com o estado atual do recurso no servidor.',                                  accent: '#FF8C00', icon: '⚡' },
    410: { name: 'Recurso Removido',                      tagline: 'Dente extraído sem retorno!',                 desc: 'Este recurso foi removido permanentemente e não possui endereço alternativo.',                     accent: '#FF8C00', icon: '🪦' },
    411: { name: 'Tamanho Obrigatório',                   tagline: 'Esqueceu o peso da ficha!',                   desc: 'O servidor requer que o cabeçalho Content-Length esteja definido.',                                accent: '#FFE600', icon: '📏' },
    412: { name: 'Pré-condição Falhou',                   tagline: 'Cirurgia sem preparo adequado!',              desc: 'O servidor não atende às pré-condições especificadas na requisição.',                              accent: '#FFE600', icon: '⚠️' },
    413: { name: 'Payload Muito Grande',                  tagline: 'Raio-X não cabe na pasta!',                   desc: 'O corpo da requisição excede o tamanho máximo suportado pelo servidor.',                          accent: '#FF8C00', icon: '📦' },
    414: { name: 'URI Muito Longa',                       tagline: 'Endereço do consultório quilométrico!',       desc: 'A URI da requisição ultrapassa o limite máximo permitido pelo servidor.',                          accent: '#FFE600', icon: '📍' },
    415: { name: 'Tipo de Mídia Não Suportado',           tagline: 'Equipamento incompatível!',                   desc: 'O servidor não suporta o tipo de mídia enviado nesta requisição.',                                 accent: '#FFE600', icon: '🎞️' },
    416: { name: 'Intervalo Inválido',                    tagline: 'Ângulo do raio-X impossível!',                desc: 'O intervalo de bytes solicitado não pode ser satisfeito pelo servidor.',                           accent: '#FFE600', icon: '📐' },
    417: { name: 'Expectativa Falhou',                    tagline: 'O paciente esperava um milagre!',             desc: 'O servidor não consegue cumprir os requisitos do cabeçalho Expect.',                               accent: '#FFE600', icon: '💭' },
    418: { name: 'Sou um Bule de Chá!',                  tagline: 'Me peça chá, não café!',                      desc: 'Recusando preparar café — isso é um bule! (RFC 2324 · Brincadeira do Dia da Mentira)',              accent: '#FF8C00', icon: '🫖' },
    421: { name: 'Requisição Mal Direcionada',            tagline: 'Consultório errado, vizinho!',                desc: 'A requisição foi enviada a um servidor que não pode processar a resposta.',                         accent: '#FFE600', icon: '🗺️' },
    422: { name: 'Entidade Não Processável',              tagline: 'Ficha com dados incorretos!',                 desc: 'A requisição está bem formatada, mas contém erros semânticos impeditivos.',                        accent: '#FF8C00', icon: '📝' },
    423: { name: 'Recurso Bloqueado',                     tagline: 'Sala de cirurgia trancada!',                  desc: 'O recurso solicitado está bloqueado e temporariamente inacessível.',                               accent: '#FF8C00', icon: '🔒' },
    424: { name: 'Dependência Falhou',                    tagline: 'O auxiliar não compareceu!',                  desc: 'A requisição falhou pois depende de outra que também falhou.',                                     accent: '#FFE600', icon: '🔗' },
    425: { name: 'Muito Cedo',                            tagline: 'A anestesia ainda não fez efeito!',           desc: 'O servidor recusa processar esta requisição pois pode ser reproduzida prematuramente.',             accent: '#FFE600', icon: '⏳' },
    426: { name: 'Upgrade Necessário',                    tagline: 'Equipamento precisa de atualização!',         desc: 'O cliente precisa mudar para o protocolo especificado pelo servidor.',                             accent: '#FFE600', icon: '⬆️' },
    428: { name: 'Pré-condição Necessária',               tagline: 'Assine o termo de consentimento!',            desc: 'O servidor exige que a requisição seja condicional para evitar conflitos.',                         accent: '#FFE600', icon: '✍️' },
    429: { name: 'Muitas Requisições',                    tagline: 'Calma, um paciente de cada vez!',             desc: 'Você enviou requisições demais em pouco tempo. Aguarde e tente novamente.',                        accent: '#FF8C00', icon: '🚦' },
    431: { name: 'Cabeçalhos Muito Grandes',              tagline: 'Prontuário não cabe na gaveta!',              desc: 'O servidor rejeita a requisição pois os campos do cabeçalho excedem o limite.',                   accent: '#FFE600', icon: '📚' },
    451: { name: 'Indisponível por Razões Legais',        tagline: 'Processo judicial em andamento!',             desc: 'Este recurso foi removido por determinação legal ou regulatória.',                                  accent: '#FF8C00', icon: '⚖️' },
    500: { name: 'Erro Interno do Servidor',              tagline: 'O dentista tá com dor de dente!',             desc: 'Ocorreu um erro inesperado. Nossa equipe técnica já foi notificada.',                               accent: '#FF3D00', icon: '💥' },
    501: { name: 'Não Implementado',                      tagline: 'Procedimento ainda não disponível!',          desc: 'O servidor não possui a funcionalidade necessária para processar esta requisição.',                 accent: '#FF3D00', icon: '🚧' },
    502: { name: 'Gateway Inválido',                      tagline: 'A recepção recebeu uma resposta errada!',     desc: 'O servidor recebeu uma resposta inválida do servidor upstream.',                                    accent: '#FF3D00', icon: '📡' },
    503: { name: 'Serviço Indisponível',                  tagline: 'Consultório fechado para manutenção!',        desc: 'O serviço está temporariamente indisponível por sobrecarga ou manutenção programada.',             accent: '#FF3D00', icon: '🔧' },
    504: { name: 'Timeout do Gateway',                    tagline: 'O servidor demorou demais para responder!',   desc: 'O gateway não recebeu resposta a tempo do servidor de origem.',                                    accent: '#FF3D00', icon: '⏱️' },
    505: { name: 'Versão HTTP Não Suportada',             tagline: 'Protocolo do século passado!',                desc: 'O servidor não suporta a versão do protocolo HTTP usada na requisição.',                          accent: '#FF3D00', icon: '🕰️' },
    506: { name: 'Variante Também Negocia',               tagline: 'Loop no consultório!',                        desc: 'Erro de configuração: o servidor entrou em ciclo de negociação de conteúdo.',                      accent: '#FF3D00', icon: '🌀' },
    507: { name: 'Armazenamento Insuficiente',            tagline: 'Arquivo cheio de raios-X!',                   desc: 'O servidor não tem espaço suficiente para completar a requisição.',                               accent: '#FF3D00', icon: '💾' },
    508: { name: 'Loop Detectado',                        tagline: 'Dente mordendo a própria cauda!',             desc: 'O servidor detectou um loop infinito ao processar esta requisição.',                               accent: '#FF3D00', icon: '🔄' },
    510: { name: 'Extensão Necessária',                   tagline: 'Faltam peças no equipamento!',                desc: 'O servidor requer extensões adicionais não informadas pelo cliente.',                             accent: '#FF3D00', icon: '🔌' },
    511: { name: 'Autenticação de Rede Necessária',       tagline: 'Faça login no Wi-Fi do consultório!',         desc: 'Você precisa se autenticar para obter acesso à rede.',                                             accent: '#FF3D00', icon: '📶' },
  };

  var e = ERRORS[code] || {
    name: 'Erro Desconhecido', tagline: 'Algo deu errado por aqui!',
    desc: 'Ocorreu um erro inesperado no servidor.', accent: '#FF3D00', icon: '⚠️'
  };

  document.title = 'DentiBot — ' + code;

  var style = document.createElement('style');
  style.textContent = [
    '@import url("https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;700;900&family=Inter:wght@400;600;700&display=swap");',
    ':root { --accent: ' + e.accent + '; }',
    '* { box-sizing: border-box; margin: 0; padding: 0; }',
    'body { font-family: "Inter", sans-serif; background-color: #f4f4f0;',
    '  background-image: radial-gradient(#00000018 1px, transparent 1px);',
    '  background-size: 20px 20px; min-height: 100vh;',
    '  display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 24px; }',
    '.fd { font-family: "Space Grotesk", sans-serif; }',
    '.ns { box-shadow: 6px 6px 0 0 #000; }',
    '.ns-sm { box-shadow: 4px 4px 0 0 #000; }',
    '.btn { border: 4px solid #000; font-weight: 900; text-transform: uppercase;',
    '  letter-spacing: .05em; padding: 12px 24px; cursor: pointer;',
    '  display: inline-flex; align-items: center; gap: 8px;',
    '  text-decoration: none; font-size: 13px; color: #000;',
    '  transition: transform .1s, box-shadow .1s; }',
    '.btn:hover { transform: translate(2px,2px); box-shadow: 4px 4px 0 0 #000; }',
    '.btn:active { transform: translate(6px,6px); box-shadow: none; }',
    '.qbtn { border: 3px solid #000; font-weight: 900; text-transform: uppercase;',
    '  font-size: 11px; padding: 6px 12px; text-decoration: none; color: #000;',
    '  background: #fff; transition: transform .1s, box-shadow .1s, background .1s; }',
    '.qbtn:hover { transform: translate(1px,1px); box-shadow: 2px 2px 0 0 #000; background: var(--accent); }',
    '@keyframes float { 0%,100%{transform:translateY(0) rotate(-3deg)} 50%{transform:translateY(-16px) rotate(3deg)} }',
    '.float { animation: float 3s ease-in-out infinite; }',
    '@keyframes shake { 0%,100%{transform:translateX(0)} 25%{transform:translateX(-6px)} 75%{transform:translateX(6px)} }',
    '.shake { animation: shake .4s ease; }',
  ].join('\n');
  document.head.appendChild(style);

  document.body.innerHTML = '<div style="text-align:center;max-width:560px;width:100%;">'

    + '<div id="err-icon" class="ns float" style="height:128px;width:128px;background:' + e.accent + ';border:4px solid #000;margin:0 auto 32px;display:flex;align-items:center;justify-content:center;border-radius:4px;cursor:pointer;">'
    + '<span style="font-size:3.5rem;">' + e.icon + '</span>'
    + '</div>'

    + '<div style="position:relative;margin-bottom:16px;">'
    + '<p class="fd" style="font-size:120px;line-height:1;font-weight:900;color:#000;opacity:.08;user-select:none;">' + code + '</p>'
    + '<div style="position:absolute;top:0;left:0;right:0;bottom:0;display:flex;align-items:center;justify-content:center;">'
    + '<div class="ns" style="background:' + e.accent + ';border:4px solid #000;padding:8px 24px;">'
    + '<p class="fd" style="font-size:48px;font-weight:900;">' + code + '</p>'
    + '</div></div></div>'

    + '<h1 class="fd" style="font-size:22px;font-weight:900;text-transform:uppercase;letter-spacing:-.02em;margin-top:32px;margin-bottom:8px;">' + e.name + '</h1>'
    + '<p style="font-weight:700;color:#6b7280;text-transform:uppercase;font-size:12px;background:#fff;border:2px solid #000;padding:4px 12px;display:inline-block;margin-bottom:20px;">' + e.tagline + '</p>'
    + '<p style="color:#6b7280;font-size:14px;font-weight:600;max-width:420px;margin:0 auto 32px;">' + e.desc + '</p>'

    + '<div style="display:flex;flex-wrap:wrap;gap:12px;justify-content:center;margin-bottom:40px;">'
    + '<a href="javascript:history.back()" class="btn ns" style="background:#fff;">← Voltar</a>'
    + '<a href="selectperfil.html" class="btn ns" style="background:' + e.accent + ';">🏠 Ir ao Painel</a>'
    + '</div>'

    + '<div style="border-top:4px solid #000;padding-top:24px;">'
    + '<p style="font-weight:900;font-size:11px;text-transform:uppercase;letter-spacing:.1em;color:#9ca3af;margin-bottom:16px;">Ou acesse diretamente:</p>'
    + '<div style="display:flex;flex-wrap:wrap;gap:8px;justify-content:center;">'
    + '<a href="login.html" class="qbtn">Login</a>'
    + '<a href="dentista.html" class="qbtn">🦷 Dentista</a>'
    + '<a href="recpecionista.html" class="qbtn">📅 Recepção</a>'
    + '<a href="cliente.html" class="qbtn">👤 Paciente</a>'
    + '<a href="almoxarifado.html" class="qbtn">📦 Estoque</a>'
    + '<a href="coordenador.html" class="qbtn">⚙️ Coord.</a>'
    + '<a href="financeiro.html" class="qbtn">💰 Financeiro</a>'
    + '<a href="relatorios.html" class="qbtn">📊 Relatórios</a>'
    + '</div></div>'

    + '<p style="font-weight:700;font-size:11px;text-transform:uppercase;color:#9ca3af;margin-top:40px;">DentiBot v1.2.0 · Erro HTTP ' + code + '</p>'
    + '</div>';

  var icon = document.getElementById('err-icon');
  if (icon) {
    icon.addEventListener('click', function () {
      this.classList.remove('shake');
      void this.offsetWidth;
      this.classList.add('shake');
      var el = this;
      setTimeout(function () { el.classList.remove('shake'); }, 400);
    });
  }
})();
