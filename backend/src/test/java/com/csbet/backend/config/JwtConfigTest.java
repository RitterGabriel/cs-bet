package com.csbet.backend.config;

import com.csbet.backend.auth.TokenService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;
import static org.assertj.core.api.Assertions.*;

class JwtConfigTest {
    private final JwtConfig config = new JwtConfig();

    @Test
    void rejectsWeakOrBlankSecret() {
        for (String secret : new String[]{"", "short", " ".repeat(32)}) {
            assertThatThrownBy(() -> config.jwtKey(secret)).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void rejectsTokensSignedByAnotherKey() {
        var first = config.jwtKey(UUID.randomUUID().toString());
        var second = config.jwtKey(UUID.randomUUID().toString());
        String token = new TokenService(config.jwtEncoder(first)).issue(1L).accessToken();
        assertThatThrownBy(() -> config.jwtDecoder(second).decode(token)).isInstanceOf(JwtException.class);
    }
}
