package com.csbet.backend.market;

import com.csbet.backend.casecatalog.*;
import com.csbet.backend.skin.*;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;

class MarketCatalogQueryTest {
    @Test
    void allSortAndCombinedFilterQueriesAreValidWithOracleDialect() {
        var registry = new StandardServiceRegistryBuilder().applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .applySetting("hibernate.dialect", "org.hibernate.dialect.OracleDialect").applySetting("hibernate.hbm2ddl.auto", "none").build();
        try {
            var sources = new MetadataSources(registry);
            for (var entity : new Class<?>[]{CosmeticItem.class,Rarity.class,WeaponCase.class,MarketVariant.class}) sources.addAnnotatedClass(entity);
            try (var factory=sources.buildMetadata().buildSessionFactory(); var session=factory.openSession()) {
                for (var sort : new String[]{"name","name_desc","price_asc","price_desc"}) {
                    for (var weapon : new boolean[]{false,true}) for (var rarity : new boolean[]{false,true}) for (var wear : new boolean[]{false,true}) {
                        session.createQuery(MarketCatalogRepository.skinQuery(sort,weapon,rarity,wear),Object[].class);
                        session.createQuery("select count(s)"+MarketCatalogRepository.skinWhere(weapon,rarity,wear).replace("join fetch","join"),Long.class);
                    }
                }
                session.createQuery(MarketCatalogRepository.caseQuery("price_asc"),WeaponCase.class);
                session.createQuery(MarketCatalogRepository.caseQuery("price_desc"),WeaponCase.class);
            }
        } finally { StandardServiceRegistryBuilder.destroy(registry); }
    }
}
