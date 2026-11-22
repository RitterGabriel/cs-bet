package com.csbet.backend.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String COOKIE_NAME = "CSBET_SESSION";
    private final AuthService service;
    private final boolean secureCookie;

    public AuthController(AuthService service,
                          @Value("${csbet.auth.cookie-secure:false}") boolean secureCookie) {
        this.service = service;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                                HttpServletRequest http) {
        var login = service.login(request);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, cookie(login.accessToken(), login.expiresIn(), http).toString())
                .body(login);
    }

    @GetMapping("/session")
    public ResponseEntity<LoginResponse> session(
            @CookieValue(name = COOKIE_NAME, defaultValue = "") String token,
            HttpServletRequest http) {
        requireSessionHeader(http);
        // Restaurar devolve o mesmo JWT, sem renovar sua validade.
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.restore(token));
    }

    @PostMapping("/logout")
    public ResponseEntity<java.util.Map<String, String>> logout(HttpServletRequest http) {
        requireSessionHeader(http);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, cookie("", 0, http).toString())
                .body(java.util.Map.of("message", "Sessão encerrada."));
    }

    private void requireSessionHeader(HttpServletRequest request) {
        // Um site externo não pode enviar este cabeçalho sem autorização CORS.
        if (!"1".equals(request.getHeader("X-CSBET-Session")))
            throw new AccessDeniedException("Requisição de sessão inválida.");
    }

    private ResponseCookie cookie(String value, long maxAge, HttpServletRequest http) {
        return ResponseCookie.from(COOKIE_NAME, value).httpOnly(true)
                .secure(secureCookie || http.isSecure()).sameSite("Strict")
                .path("/api/auth").maxAge(maxAge).build();
    }

}
