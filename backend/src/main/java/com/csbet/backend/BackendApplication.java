package com.csbet.backend;

import com.csbet.backend.casecatalog.CatalogImportApplication;
import java.util.Arrays;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {
    public static void main(String[] args) {
        if (Arrays.asList(args).contains("--catalog-import")) {
            System.exit(CatalogImportApplication.run(args));
            return;
        }
        if (Arrays.asList(args).contains("--prices-update")) {
            System.exit(com.csbet.backend.pricing.PriceUpdateApplication.run(args));
            return;
        }
        SpringApplication.run(BackendApplication.class, args);
    }
}
