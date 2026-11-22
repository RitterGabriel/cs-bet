package com.csbet.backend.casecatalog;

import com.csbet.backend.BackendApplication;
import java.nio.file.Path;
import java.util.*;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * mvn spring-boot:run -Dspring-boot.run.arguments="--catalog-import --catalog.dry-run=true"
 * Remova dry-run para migrar/importar no CSBET com DB_USERNAME/DB_PASSWORD do projeto.
 * Snapshot é o padrão. Rede: --catalog.source=remote --catalog.revision=<sha>.
 * Arquivos: --catalog.source=files --catalog.crates=/path/crates.json --catalog.skins=/path/skins.json
 * com --catalog.revision=<sha> e --catalog.language=en (metadados obrigatórios para arquivos próprios).
 */
public final class CatalogImportApplication {
    private CatalogImportApplication() { }
    public static int run(String[] args) {
        try {
            var loader = new CatalogSourceLoader();
            String revision = option(args, "catalog.revision", CatalogSourceLoader.REVISION);
            String language = option(args, "catalog.language", "pt-BR");
            SourceCatalog source = switch (option(args, "catalog.source", "snapshot")) {
                case "snapshot" -> loader.bundled();
                case "remote" -> loader.remote(revision, language);
                case "files" -> loader.local(Path.of(option(args, "catalog.crates", "crates.json")),
                        Path.of(option(args, "catalog.skins", "skins.json")),
                        option(args, "catalog.revision", ""), option(args, "catalog.language", ""));
                default -> throw new SourceDataException("Origem inválida. Use snapshot, remote ou files.");
            };
            int links = source.cases().stream().mapToInt(box -> box.links().size()).sum();
            if (option(args, "catalog.dry-run", "false").equals("true")) {
                System.out.printf("Validação sem Oracle: %d caixas, %d itens, %d associações; idioma=%s; revisão=%s%n",
                        source.cases().size(), source.items().size(), links, source.language(), source.revision());
                return 0;
            }
            try (var context = new SpringApplicationBuilder(BackendApplication.class)
                    .web(WebApplicationType.NONE).profiles("catalog-import").run(args)) {
                var summary = context.getBean(CatalogImportService.class).importCatalog(source);
                System.out.printf("Importação concluída: %d caixas, %d itens, %d associações; idioma=%s; revisão=%s%n",
                        summary.cases(), summary.items(), summary.associations(), summary.language(), summary.revision());
            }
            return 0;
        } catch (SourceDataException exception) {
            System.err.println(exception.getMessage());
        } catch (Exception exception) {
            System.err.println("Importação não concluída. Verifique a conexão do projeto, o schema CSBET e as migrations. Dados de usuários não são alterados pelo importador.");
        }
        return 1;
    }
    private static String option(String[] args, String name, String fallback) {
        String prefix = "--" + name + "=";
        return Arrays.stream(args).filter(arg -> arg.startsWith(prefix)).map(arg -> arg.substring(prefix.length())).findFirst().orElse(fallback);
    }
}
