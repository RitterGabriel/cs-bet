package com.csbet.backend.pricing;

import java.sql.SQLException;
import org.hibernate.AssertionFailure;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PriceUpdateApplicationTest {
    @Test
    void ormFailureDoesNotBlameConnectivityOrAnAlreadyAppliedMigration() {
        AssertionFailure failure = org.mockito.Mockito.mock(AssertionFailure.class);
        org.mockito.Mockito.when(failure.getMessage()).thenReturn("null identifier secret-token private-password");
        String message = PriceUpdateApplication.failureMessage(new RuntimeException(failure));
        assertThat(message).contains("AssertionFailure", "mapeamento")
                .doesNotContain("conexão", "aplique a migração V4", "secret-token", "private-password");
    }
    @Test
    void distinguishesConnectionIntegritySchemaAndUnexpectedFailuresWithoutLeakingDetails() {
        assertThat(PriceUpdateApplication.failureMessage(new SQLException("private detail", "08006")))
                .contains("comunicação").doesNotContain("private detail");
        assertThat(PriceUpdateApplication.failureMessage(new org.springframework.dao.DataIntegrityViolationException("secret")))
                .contains("integridade").doesNotContain("secret");
        assertThat(PriceUpdateApplication.failureMessage(new org.hibernate.tool.schema.spi.SchemaManagementException("secret")))
                .contains("validação").doesNotContain("secret");
        assertThat(PriceUpdateApplication.failureMessage(new IllegalStateException("secret")))
                .contains("IllegalStateException").doesNotContain("secret");
    }
}
