package com.csbet.backend.auth;
import com.csbet.backend.repository.UserRepository;
import com.csbet.backend.entity.User;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
@Service
public class AuthService {
    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final TokenService tokens;
    private final String dummyHash;
    private final org.springframework.security.oauth2.jwt.JwtDecoder decoder;
    public AuthService(UserRepository repository, PasswordEncoder encoder, TokenService tokens, org.springframework.security.oauth2.jwt.JwtDecoder decoder) {
        this.repository = repository;
        this.decoder = decoder;
        this.encoder = encoder;
        this.tokens = tokens;
        this.dummyHash = encoder.encode(UUID.randomUUID().toString());
    }
    @Transactional(readOnly=true)
    public LoginResponse login(LoginRequest request) {
        var user = repository.findByEmail(request.email());
        // Executa BCrypt também quando o e-mail não existe.
        boolean matches = encoder.matches(request.password(), user.map(User::getPasswordHash).orElse(dummyHash));
        if (!matches || user.isEmpty()) throw new BadCredentialsException("Credenciais inválidas.");
        return tokens.issue(user.get().getId());
    }
    @Transactional(readOnly=true)
    public LoginResponse restore(String token) {
        try {
            var jwt = decoder.decode(token);
            long remaining = java.time.Duration.between(java.time.Instant.now(), jwt.getExpiresAt()).getSeconds();
            if (remaining <= 0 || !repository.existsById(Long.valueOf(jwt.getSubject())))
                throw new BadCredentialsException("Sessão inválida ou expirada.");
            return new LoginResponse(token, "Bearer", remaining);
        } catch (org.springframework.security.oauth2.jwt.JwtException | IllegalArgumentException exception) {
            throw new BadCredentialsException("Sessão inválida ou expirada.");
        }
    }
}
