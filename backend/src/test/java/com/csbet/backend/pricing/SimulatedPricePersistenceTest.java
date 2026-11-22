package com.csbet.backend.pricing;

import com.csbet.backend.skin.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Hibernate/Spring Data reais; JDBC simulado, sem Oracle e sem executar DDL. */
class SimulatedPricePersistenceTest {
    @Test
    void savesNewPriceForPersistedSkin() throws Exception {
        java.util.List<String> sqlStatements = new java.util.ArrayList<>();
        Connection connection = mock(Connection.class);
        org.mockito.stubbing.Answer<PreparedStatement> prepare = call -> {
            sqlStatements.add(call.getArgument(0));
            PreparedStatement statement = mock(PreparedStatement.class);
            ResultSet result = mock(ResultSet.class);
            when(statement.executeQuery()).thenReturn(result);
            when(statement.executeUpdate()).thenReturn(1);
            return statement;
        };
        when(connection.prepareStatement(anyString())).thenAnswer(prepare);
        when(connection.prepareStatement(anyString(), anyInt(), anyInt())).thenAnswer(prepare);
        when(connection.getAutoCommit()).thenReturn(false);
        var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .applySetting("hibernate.dialect", "org.hibernate.dialect.OracleDialect")
                .applySetting("hibernate.hbm2ddl.auto", "none").build();
        try {
            var sources = new MetadataSources(registry).addAnnotatedClass(CosmeticItem.class)
                    .addAnnotatedClass(Rarity.class).addAnnotatedClass(SimulatedPrice.class);
            try (var factory = sources.buildMetadata().buildSessionFactory();
                 var session = factory.withOptions().connection(connection).openSession()) {
                var transaction = session.beginTransaction();
                try {
                    var skin = session.getReference(CosmeticItem.class, 42L);
                    var price = new SimulatedPrice(skin, new BigDecimal("100.00"), LocalDate.of(2026, 11, 21),
                            LocalDateTime.of(2026, 11, 21, 12, 0));
                    var repository = new SimpleJpaRepository<>(SimulatedPrice.class, session);
                    assertThat(price.isNew()).isTrue();
                    var saved = repository.save(price);
                    assertThat(saved).isSameAs(price);
                    session.flush();
                    assertThat(saved.getSkinId()).isEqualTo(42L);
                    assertThat(session.contains(saved)).isTrue();
                    assertThat(saved.isNew()).isFalse();
                    assertThat(repository.save(saved)).isSameAs(saved);
                    session.flush();
                    assertThat(sqlStatements.stream().filter(sql -> sql.toLowerCase(java.util.Locale.ROOT)
                            .startsWith("insert into csbet.csbet_simulated_prices")).count()).isEqualTo(1);
                    assertThat(sqlStatements).noneMatch(sql -> sql.toLowerCase(java.util.Locale.ROOT).contains("new_entity"));
                } finally { transaction.rollback(); }
            }
        } finally { StandardServiceRegistryBuilder.destroy(registry); }
    }
}
