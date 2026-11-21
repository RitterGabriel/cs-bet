package com.csbet.backend.auth;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
@Service
public class TokenService {
    private final JwtEncoder encoder;
    public TokenService(JwtEncoder encoder) { this.encoder = encoder; }
    public LoginResponse issue(Long userId) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().subject(userId.toString()).issuedAt(now)
                .expiresAt(now.plusSeconds(3600)).claim("token_type", "access").build();
        var header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return new LoginResponse(encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue(), "Bearer", 3600);
    }
}
