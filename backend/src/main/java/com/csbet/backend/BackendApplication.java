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
        SpringApplication.run(BackendApplication.class, args);
    }
}
