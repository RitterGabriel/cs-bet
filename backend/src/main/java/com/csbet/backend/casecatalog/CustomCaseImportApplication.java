package com.csbet.backend.casecatalog;
import com.csbet.backend.BackendApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
public final class CustomCaseImportApplication {
    private CustomCaseImportApplication() { }
    public static int run(String[] args) {
        try(var context=new SpringApplicationBuilder(BackendApplication.class).web(WebApplicationType.NONE).profiles("catalog-import").run(args)) {
            int count=context.getBean(CustomCaseSeedService.class).seed(CustomCaseManifest.bundled());
            System.out.println("Caixas personalizadas cadastradas: "+count);return 0;
        }catch(Exception failure){System.err.println("Personalizadas não cadastradas. Verifique V11, catálogo, variantes e definições; nenhum saldo foi alterado.");return 1;}
    }
}
