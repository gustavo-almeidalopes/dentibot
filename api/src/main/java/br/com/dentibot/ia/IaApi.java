package br.com.dentibot.ia;

import java.util.Set;

/**
 * Porta do gateway de IA (ST-61). Todo módulo que fala com modelo de linguagem
 * passa por aqui — nenhum chama provedor direto —, e por isso os controles do
 * Doc 03 valem para todos: redação de PII, trilha, cota, desligamento e
 * confirmação humana.
 *
 * <p>A permissão de DOMÍNIO é de quem chama: o prontuário confere se o dentista
 * alcança o paciente antes de pedir a nota. O gateway confere o que é dele —
 * recurso ligado, cota, provedor.
 */
public interface IaApi {

    /** Os recursos que existem; é a lista que a clínica pode ligar e desligar. */
    Set<String> RECURSOS = Set.of("nota_clinica", "plano_duas_linguagens");

    RespostaDeIa executar(PedidoDeIa pedido);

    /**
     * A decisão humana sobre uma sugestão: aceita (e onde entrou) ou descartada.
     * Uma por chamada; nada sugerido vira registro oficial sem ela.
     */
    void confirmar(long idChamada, boolean aceita, String registro);
}
