package com.csbet.backend.wallet;

import com.csbet.backend.entity.User;
import com.csbet.backend.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;

/** Optativo: exige V7 já aplicada no Oracle do projeto. Usa apenas usuários criados pelo teste. */
@EnabledIfEnvironmentVariable(named = "CSBET_ORACLE_TEST", matches = "true")
@SpringBootTest(properties = {"csbet.pricing.schedule-enabled=false", "csbet.market.enabled=false"})
class WalletOracleTest {
    @Autowired WalletService wallet;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate transaction;
    private Long ownerId;
    private Long otherId;
    @BeforeEach
    void setup() {
        assertThat(jdbc.queryForObject("SELECT SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') FROM DUAL", String.class)).isEqualTo("CSBET");
        ownerId = createUser(); otherId = createUser();
    }
    private Long createUser() {
        String name = "wallet-test-" + UUID.randomUUID();
        return users.saveAndFlush(new User(name, name + "@example.invalid", "unused-test-hash", BigDecimal.ZERO, LocalDateTime.now())).getId();
    }
    @AfterEach
    void cleanup() {
        for (Long id : Arrays.asList(ownerId, otherId)) {
            if (id == null) continue;
            jdbc.update("DELETE FROM CSBET_WALLET_MOVEMENTS WHERE USER_ID = ?", id);
            users.deleteById(id);
        }
    }
    @Test
    void concurrentAdditionsDoNotLoseUpdatesAndRemainIsolated() throws Exception {
        int count = 8;
        try (var executor = Executors.newFixedThreadPool(count)) {
            var start = new CountDownLatch(1);
            var futures = new ArrayList<Future<?>>();
            for (int i = 0; i < count; i++) futures.add(executor.submit(() -> {
                start.await();
                return wallet.add(ownerId, new AddCreditsRequest(new BigDecimal("10.25"), UUID.randomUUID()));
            }));
            start.countDown();
            for (var future : futures) future.get(30, TimeUnit.SECONDS);
        }
        assertThat(new BigDecimal(wallet.balance(ownerId).balance())).isEqualByComparingTo("82.00");
        assertThat(wallet.history(ownerId, 0).totalElements()).isEqualTo(count);
        // O Oracle pode retornar escalas diferentes; a sequência deve ser numericamente exata.
        assertThat(wallet.history(ownerId, 0).content())
                .extracting(movement -> new BigDecimal(movement.resultingBalance()))
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("82.00"), new BigDecimal("71.75"),
                        new BigDecimal("61.50"), new BigDecimal("51.25"), new BigDecimal("41.00"),
                        new BigDecimal("30.75"), new BigDecimal("20.50"), new BigDecimal("10.25"));
        assertThat(new BigDecimal(wallet.balance(otherId).balance())).isEqualByComparingTo("0.00");
        assertThat(wallet.history(otherId, 0).content()).isEmpty();
    }
    @Test
    void concurrentRetriesAreRecordedOnce() throws Exception {
        UUID request = UUID.randomUUID();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            Callable<WalletResponse.Balance> retry = () -> {
                start.await();
                return wallet.add(ownerId, new AddCreditsRequest(new BigDecimal("10.00"), request));
            };
            var first = executor.submit(retry);
            var second = executor.submit(retry);
            start.countDown();
            first.get(30, TimeUnit.SECONDS); second.get(30, TimeUnit.SECONDS);
        }
        assertThat(new BigDecimal(wallet.balance(ownerId).balance())).isEqualByComparingTo("10.00");
        assertThat(wallet.history(ownerId, 0).totalElements()).isEqualTo(1);
    }
    @Test
    void rollbackRevertsBothBalanceAndMovement() {
        transaction.executeWithoutResult(status -> {
            wallet.add(ownerId, new AddCreditsRequest(new BigDecimal("25.00"), UUID.randomUUID()));
            assertThat(new BigDecimal(wallet.balance(ownerId).balance())).isEqualByComparingTo("25.00");
            assertThat(wallet.history(ownerId, 0).totalElements()).isEqualTo(1);
            status.setRollbackOnly();
        });
        assertThat(new BigDecimal(wallet.balance(ownerId).balance())).isEqualByComparingTo("0.00");
        assertThat(wallet.history(ownerId, 0).content()).isEmpty();
    }
}
