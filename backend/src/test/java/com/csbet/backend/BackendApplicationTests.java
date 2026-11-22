package com.csbet.backend;

import com.csbet.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = "spring.autoconfigure.exclude="
        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
        + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration")
class BackendApplicationTests {
    @MockitoBean UserRepository repository;
    @MockitoBean com.csbet.backend.casecatalog.WeaponCaseRepository cases;
    @MockitoBean com.csbet.backend.casecatalog.CaseItemRepository contents;
    @MockitoBean com.csbet.backend.skin.CosmeticItemRepository items;
    @MockitoBean com.csbet.backend.skin.RarityRepository rarities;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        String secret = java.util.UUID.randomUUID().toString() + java.util.UUID.randomUUID();
        registry.add("JWT_SECRET", () -> secret);
    }

    @Test
    void contextLoads() { }
}
