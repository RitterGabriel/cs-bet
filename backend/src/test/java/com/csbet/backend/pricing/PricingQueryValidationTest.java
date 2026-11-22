package com.csbet.backend.pricing;

import com.csbet.backend.casecatalog.*;
import com.csbet.backend.entity.User;
import com.csbet.backend.inventory.*;
import com.csbet.backend.skin.*;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

/** Valida mapeamentos e HQL com o dialect Oracle, sem conexão nem execução de SQL. */
class PricingQueryValidationTest {
    @Test
    void validatesAggregationAndConstructorQueriesWithoutOracle() throws Exception {
        var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .applySetting("hibernate.dialect", "org.hibernate.dialect.OracleDialect")
                .applySetting("hibernate.hbm2ddl.auto", "none").build();
        try {
            var sources = new MetadataSources(registry);
            for (Class<?> entity : new Class<?>[]{User.class, CosmeticItem.class, Rarity.class,
                    WeaponCase.class, CaseItem.class, InventoryItem.class, SimulatedPrice.class,
                    PriceHistory.class, PriceControl.class, com.csbet.backend.market.MarketVariant.class}) sources.addAnnotatedClass(entity);
            try (var factory = sources.buildMetadata().buildSessionFactory(); var session = factory.openSession()) {
                String valuation = InventoryRepository.class.getMethod("estimateAvailable", Long.class, InventoryStatus.class, java.time.LocalDateTime.class)
                        .getAnnotation(Query.class).value();
                session.createQuery(valuation, InventoryValuation.class);
                String ranges = CaseItemRepository.class.getMethod("findPriceRanges", java.util.Collection.class)
                        .getAnnotation(Query.class).value();
                session.createQuery(ranges, CasePriceRange.class);
            }
        } finally { StandardServiceRegistryBuilder.destroy(registry); }
    }
}
