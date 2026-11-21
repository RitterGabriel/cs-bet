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
    public AuthService(UserRepository repository, PasswordEncoder encoder, TokenService tokens) {
        this.repository = repository;
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
}
