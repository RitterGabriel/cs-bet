package com.csbet.backend.controller;

import com.csbet.backend.dto.CreateUserRequest;
import com.csbet.backend.dto.UserResponse;
import com.csbet.backend.exception.ApiExceptionHandler;
import com.csbet.backend.exception.UserConflictException;
import com.csbet.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerTest {
    private UserService service;
    private MockMvc mvc;
    private static final String VALID = """
            {"username":"player","email":"player@example.com","password":"strong-password"}
            """;

    @BeforeEach
    void setUp() {
        service = mock(UserService.class);
        mvc = MockMvcBuilders.standaloneSetup(new UserController(service))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void returnsCreatedWithOnlyPublicFields() throws Exception {
        when(service.create(any(CreateUserRequest.class))).thenReturn(new UserResponse(
                1L, "player", "player@example.com", BigDecimal.ZERO, LocalDateTime.now()));
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("player"))
                .andExpect(jsonPath("$.email").value("player@example.com"))
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.*", hasSize(5)))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void rejectsInvalidFields() throws Exception {
        for (String body : new String[]{
                "{\"username\":\" \",\"email\":\"invalid\",\"password\":\"short\"}",
                "{}",
                VALID.replace("player@example.com", "invalid"),
                VALID.replace("strong-password", "short"),
                VALID.replace("player\"", "x".repeat(51) + "\""),
                VALID.replace("strong-password", "é".repeat(37)),
                "{"}) {
            mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(not(containsString("password"))));
        }
        verifyNoInteractions(service);
    }

    @Test
    void returnsConflict() throws Exception {
        when(service.create(any())).thenThrow(new UserConflictException("E-mail já cadastrado."));
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isConflict());
    }

    @Test
    void hidesUnexpectedFailureDetails() throws Exception {
        when(service.create(any())).thenThrow(new RuntimeException("sensitive database details"));
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Erro interno do servidor."));
    }
}
