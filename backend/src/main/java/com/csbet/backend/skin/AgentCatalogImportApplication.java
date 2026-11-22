package com.csbet.backend.skin;

import com.csbet.backend.BackendApplication;
import java.util.Arrays;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

public final class AgentCatalogImportApplication {
    private AgentCatalogImportApplication() { }
    public static int run(String[] args) {
        try {
            var agents = AgentCatalogSource.bundled(); // Validação anterior a qualquer conexão ou transação.
            if (Arrays.asList(args).contains("--agents.dry-run=true")) {
                System.out.printf("Validação sem Oracle: %d agentes; equipes e raridades verificadas.%n",agents.size());
                return 0;
            }
            try (var context = new SpringApplicationBuilder(BackendApplication.class).web(WebApplicationType.NONE)
                    .profiles("agent-import").run(args)) {
                int count = context.getBean(AgentCatalogImportService.class).importAgents(agents);
                System.out.printf("Importação de agentes concluída: %d itens. Caixas oficiais e seus vínculos preservados.%n",count);
            }
            return 0;
        } catch (Exception failure) {
            System.err.println("Importação de agentes não concluída. Tipo: "+failure.getClass().getSimpleName()+". Confira o snapshot e a configuração do Oracle; migrações não são executadas por este comando.");
            return 1;
        }
    }
}
