package com.csbet.backend.auth;
import jakarta.validation.constraints.*;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
public record LoginRequest(@NotBlank @Email @Size(max=150) String email,
                           @NotBlank @Size(max=72) String password) {
    public LoginRequest { email = email == null ? null : email.strip().toLowerCase(Locale.ROOT); }
    @AssertTrue(message="Senha inválida")
    public boolean isPasswordWithinByteLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
    @Override public String toString() { return "LoginRequest[redacted]"; }
}
