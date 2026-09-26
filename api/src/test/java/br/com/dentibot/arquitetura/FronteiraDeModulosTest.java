package br.com.dentibot.arquitetura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Set;

/**
 * A regra que faz "módulos antes de serviços" ser verdade em vez de intenção.
 *
 * <p>Um módulo só enxerga do outro o pacote raiz — onde vivem a porta e os
 * records que ela usa. {@code domain}, {@code application},
 * {@code infrastructure} e {@code interfaces} são privados.
 *
 * <p>Sem este teste, o primeiro import cruzado entra numa sexta-feira, parece
 * inofensivo, e dois anos depois ninguém consegue extrair a agenda sem
 * reescrever o domínio inteiro.
 */
@AnalyzeClasses(
        packages = "br.com.dentibot",
        importOptions = ImportOption.DoNotIncludeTests.class)
class FronteiraDeModulosTest {

    private static final Set<String> MODULOS = Set.of(
            "clinicas", "identidade", "pacientes", "agenda", "prontuario",
            "orcamento", "financeiro", "billing", "estoque", "lgpd", "auditoria");

    private static final Set<String> CAMADAS_INTERNAS = Set.of(
            "domain", "application", "infrastructure", "interfaces");

    @ArchTest
    static final ArchRule moduloNaoAlcancaOInteriorDeOutro =
            noClasses().should(new ArchCondition<>("alcançar o interior de outro módulo") {
                @Override
                public void check(JavaClass classe, ConditionEvents eventos) {
                    String meuModulo = moduloDe(classe.getPackageName());
                    if (meuModulo == null) {
                        return;
                    }
                    for (Dependency dep : classe.getDirectDependenciesFromSelf()) {
                        String pacoteAlvo = dep.getTargetClass().getPackageName();
                        String moduloAlvo = moduloDe(pacoteAlvo);

                        if (moduloAlvo != null
                                && !moduloAlvo.equals(meuModulo)
                                && ehCamadaInterna(pacoteAlvo)) {
                            eventos.add(SimpleConditionEvent.violated(classe, """
                                    %s alcança %s — interior do módulo '%s'. \
                                    Converse pela porta pública (br.com.dentibot.%s) ou por evento."""
                                    .formatted(classe.getName(), dep.getTargetClass().getName(),
                                            moduloAlvo, moduloAlvo)));
                        }
                    }
                }
            });

    /** Hexagonal: a dependência aponta para dentro. O domínio não sabe que existe banco. */
    @ArchTest
    static final ArchRule dominioNaoConheceInfraestrutura =
            noClasses()
                    .that().resideInAPackage("br.com.dentibot..domain..")
                    .should().dependOnClassesThat(
                            new DescribedPredicate<JavaClass>("estão em infrastructure ou interfaces") {
                                @Override
                                public boolean test(JavaClass alvo) {
                                    String p = alvo.getPackageName();
                                    return p.startsWith("br.com.dentibot")
                                            && (p.contains(".infrastructure") || p.contains(".interfaces"));
                                }
                            });

    /** SQL mora no repositório. Serviço que monta SQL é infraestrutura disfarçada. */
    @ArchTest
    static final ArchRule sqlSoNoRepositorio =
            noClasses()
                    .that().resideInAnyPackage("br.com.dentibot..domain..", "br.com.dentibot..application..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework.jdbc..", "java.sql..", "javax.sql..");

    /**
     * Controller não fala com banco: passa pelo serviço do módulo.
     *
     * <p>{@code allowEmptyShould} porque a regra precisa existir ANTES do
     * primeiro controller — é quando ela é útil. Sem isso o ArchUnit reprova a
     * regra por não ter classe para avaliar, e a reação natural seria apagá-la.
     */
    @ArchTest
    static final ArchRule httpNaoFalaComBanco =
            noClasses()
                    .that().resideInAPackage("br.com.dentibot..interfaces..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework.jdbc..", "java.sql..")
                    .allowEmptyShould(true);

    private static String moduloDe(String pacote) {
        if (!pacote.startsWith("br.com.dentibot.")) {
            return null;
        }
        String resto = pacote.substring("br.com.dentibot.".length());
        int ponto = resto.indexOf('.');
        String primeiro = ponto < 0 ? resto : resto.substring(0, ponto);
        return MODULOS.contains(primeiro) ? primeiro : null;
    }

    private static boolean ehCamadaInterna(String pacote) {
        String resto = pacote.substring("br.com.dentibot.".length());
        String[] partes = resto.split("\\.");
        return partes.length >= 2 && CAMADAS_INTERNAS.contains(partes[1]);
    }
}
