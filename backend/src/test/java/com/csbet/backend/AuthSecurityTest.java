package com.csbet.backend;

import com.csbet.backend.auth.*;
import com.csbet.backend.config.*;
import com.csbet.backend.controller.*;
import com.csbet.backend.entity.User;
import com.csbet.backend.exception.ApiExceptionHandler;
import com.csbet.backend.repository.UserRepository;
import com.csbet.backend.service.UserService;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@WebMvcTest(controllers = {AuthController.class, UserController.class, HealthController.class})
@Import({SecurityConfig.class, JwtConfig.class, PasswordConfig.class, AuthService.class,
        TokenService.class, UserService.class, ApiExceptionHandler.class, AuthSecurityTest.DeniedController.class})
class AuthSecurityTest {
    @org.springframework.test.context.DynamicPropertySource
    static void properties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        // Chave aleatória exclusiva dos testes, nunca usada pela aplicação.
        registry.add("JWT_SECRET", () -> TEST_SECRET);
    }
    private static final String TEST_SECRET = UUID.randomUUID().toString() + UUID.randomUUID();
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder passwords;
    @Autowired TokenService tokens;
    @Autowired JwtEncoder encoder;
    @Autowired JwtDecoder decoder;
    @MockitoBean UserRepository repository;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User("player", "player@example.com", passwords.encode("strong-password"),
                BigDecimal.ZERO, LocalDateTime.now());
        ReflectionTestUtils.setField(user, "id", 1L);
        when(repository.findByEmail("player@example.com")).thenReturn(Optional.of(user));
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(repository.existsById(1L)).thenReturn(true);
    }

    @Test
    void loginReturnsAccessTokenWithoutSensitiveClaims() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json").content("""
                {"email":" PLAYER@EXAMPLE.COM ","password":"strong-password"}
                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.*", hasSize(3)))
                .andExpect(header().string("Set-Cookie", allOf(containsString("HttpOnly"), containsString("SameSite=Strict"), containsString("Path=/api/auth"), containsString("Max-Age=3600"))))
                .andExpect(header().string("Cache-Control", containsString("no-store")));
        var jwt = decoder.decode(tokens.issue(1L).accessToken());
        assertThat(jwt.getSubject()).isEqualTo("1");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).toSeconds()).isEqualTo(3600);
        assertThat(jwt.getClaims()).containsOnlyKeys("sub", "iat", "exp", "token_type");
        assertThat(new LoginRequest("x@y.com", "secret").toString()).doesNotContain("secret");
        assertThat(tokens.issue(1L).toString()).doesNotContain("eyJ");
    }

    @Test
    void invalidCredentialsHaveIdenticalResponses() throws Exception {
        String previous = null;
        for (String email : List.of("player@example.com", "missing@example.com")) {
            String body = mvc.perform(post("/api/auth/login").contentType("application/json")
                    .content("{\"email\":\"" + email + "\",\"password\":\"wrong-password\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string(not(containsString(email))))
                    .andExpect(content().string(not(containsString("password"))))
                    .andReturn().getResponse().getContentAsString();
            if (previous != null) assertThat(body).isEqualTo(previous);
            previous = body;
        }
    }

    @Test
    void validatesLoginBody() throws Exception {
        for (String body : List.of("{}", "{", "{\"email\":\"invalid\",\"password\":\"x\"}",
                "{\"email\":\"player@example.com\",\"password\":\"" + "é".repeat(37) + "\"}")) {
            mvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(repository);
    }

    @Test
    void meUsesDatabaseAndNeverReturnsPassword() throws Exception {
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + tokens.issue(1L).accessToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.*", hasSize(5)))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(header().doesNotExist("Set-Cookie"));
        verify(repository).findById(1L);
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void restoresSameTokenWithoutExtendingItsExpiration() throws Exception {
        String token = tokens.issue(1L).accessToken();
        mvc.perform(get("/api/auth/session").header("X-CSBET-Session", "1")
                        .cookie(new jakarta.servlet.http.Cookie("CSBET_SESSION", token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").value(token))
                .andExpect(jsonPath("$.expiresIn", greaterThan(0)))
                .andExpect(jsonPath("$.expiresIn", lessThanOrEqualTo(3600)))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().doesNotExist("Set-Cookie"));
        verify(repository).existsById(1L);
    }

    @Test
    void restorationRejectsMissingInvalidExpiredAndDeletedUserSessions() throws Exception {
        mvc.perform(get("/api/auth/session").header("X-CSBET-Session", "1"))
                .andExpect(status().isUnauthorized());
        for (String token : List.of("invalid", signed("1", Instant.now().minusSeconds(1), "access"),
                tokens.issue(2L).accessToken())) {
            mvc.perform(get("/api/auth/session").header("X-CSBET-Session", "1")
                            .cookie(new jakarta.servlet.http.Cookie("CSBET_SESSION", token)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void cookieDoesNotAuthenticatePrivateEndpointsAndSessionRequiresCustomHeader() throws Exception {
        var cookie = new jakarta.servlet.http.Cookie("CSBET_SESSION", tokens.issue(1L).accessToken());
        mvc.perform(get("/api/users/me").cookie(cookie)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/session").cookie(cookie)).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout").cookie(cookie)).andExpect(status().isForbidden());
        mvc.perform(options("/api/auth/session").header("Origin", "https://external.example")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "X-CSBET-Session"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void logoutClearsPersistentCookieEvenAfterTokenExpires() throws Exception {
        mvc.perform(post("/api/auth/logout").header("X-CSBET-Session", "1")
                        .cookie(new jakarta.servlet.http.Cookie("CSBET_SESSION", "expired")))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", allOf(containsString("Max-Age=0"),
                        containsString("HttpOnly"), containsString("Path=/api/auth"), containsString("SameSite=Strict"))));
    }

    @Test
    void httpsLoginUsesSecureCookie() throws Exception {
        mvc.perform(post("/api/auth/login").secure(true).contentType("application/json").content("""
                {"email":"player@example.com","password":"strong-password"}
                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("Secure")));
    }

    private String signed(String subject, Instant expiration, String type) {
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(),
                JwtClaimsSet.builder().subject(subject).issuedAt(Instant.now().minusSeconds(7200))
                        .expiresAt(expiration).claim("token_type", type).build())).getTokenValue();
    }

    @Test
    void rejectsInvalidExpiredAndWrongTypeTokens() throws Exception {
        String valid = tokens.issue(1L).accessToken();
        String tampered = valid.substring(0, valid.lastIndexOf('.') + 1) + "A".repeat(43);
        for (String token : List.of("invalid", tampered,
                signed("1", Instant.now().minusSeconds(1), "access"),
                signed("1", Instant.now().plusSeconds(60), "refresh"),
                signed("not-an-id", Instant.now().plusSeconds(60), "access"))) {
            mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string(not(containsString(token))));
        }
        verifyNoInteractions(repository);
    }

    @Test
    void deletedUserCannotAccessMe() throws Exception {
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + tokens.issue(2L).accessToken()))
                .andExpect(status().isUnauthorized());
    }


    @org.springframework.web.bind.annotation.RestController
    static class DeniedController {
        @org.springframework.web.bind.annotation.GetMapping("/test/denied")
        void denied() {
            throw new org.springframework.security.access.AccessDeniedException("internal sensitive detail");
        }
    }

    @Test
    void authenticatedAccessDeniedReturns403() throws Exception {
        mvc.perform(get("/test/denied").header("Authorization", "Bearer " + tokens.issue(1L).accessToken()))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("internal"))));
    }

    @Test
    void onlySpecifiedRoutesArePublic() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
        mvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/health")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/login")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/users").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }
}
