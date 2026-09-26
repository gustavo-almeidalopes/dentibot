# Sub-processadores

A clínica é a **controladora** dos dados do paciente; o DentiBot é **operador**
(LGPD art. 5). Estes são os terceiros que processam dado em nome da clínica. A
lista existe para ser lida **antes** de assinar — a V16 decidiu que ela é
documento do repositório, não linha de banco.

| Sub-processador | Para quê | Que dado | Quando entra |
| --- | --- | --- | --- |
| Clerk | Autenticação da equipe | Nome, e-mail e sessão de quem usa o sistema — não de paciente | Sempre |
| Vercel | Hospedagem do web | Nenhum dado de paciente em repouso; tráfego HTTPS | Sempre |
| Cloudflare (R2) | Anexos clínicos (radiografia, foto, documento) | Arquivo clínico, criptografado em repouso, sem URL pública | Quando a clínica anexa arquivo |
| Sentry | Erros da aplicação | Pilha de erro com CPF, e-mail e telefone removidos; ids de clínica e usuário | Se `SENTRY_DSN` estiver configurado |
| Anthropic | Recursos de IA (rascunho de nota, plano em duas linguagens) | Texto clínico **redigido**: sem nome, CPF, telefone ou e-mail | Se `DENTIBOT_IA_CHAVE` estiver configurado **e** a clínica não desligou o recurso |

Todo sub-processador novo entra nesta tabela no mesmo PR que o integra.
