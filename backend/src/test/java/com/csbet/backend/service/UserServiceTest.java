package com.csbet.backend.service;

import com.csbet.backend.dto.CreateUserRequest;
import com.csbet.backend.entity.User;
import com.csbet.backend.exception.UserConflictException;
import com.csbet.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.sql.SQLException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserServiceTest {
    private UserRepository repository;
    private UserService service;
    private final CreateUserRequest request = new CreateUserRequest(" player ", " PLAYER@EXAMPLE.COM ", "strong-password");

    @BeforeEach
    void setUp() {
        repository = mock(UserRepository.class);
        service = new UserService(repository, new BCryptPasswordEncoder());
    }

    @Test
    void createsUserWithNormalizedDataAndHashedPassword() {
        when(repository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            String hash = (String) ReflectionTestUtils.getField(user, "passwordHash");
            assertThat(hash).isNotEqualTo(request.password());
            assertThat(new BCryptPasswordEncoder().matches(request.password(), hash)).isTrue();
            ReflectionTestUtils.setField(user, "id", 1L);
            return user;
        });
        var response = service.create(request);
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("player");
        assertThat(response.email()).isEqualTo("player@example.com");
        assertThat(response.balance()).isZero();
        assertThat(response.createdAt()).isNotNull();
        verify(repository).existsByUsername("player");
        verify(repository).existsByEmail("player@example.com");
    }

    @Test
    void rejectsDuplicateUsername() {
        when(repository.existsByUsername("player")).thenReturn(true);
        assertThatThrownBy(() -> service.create(request)).isInstanceOf(UserConflictException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsDuplicateEmail() {
        when(repository.existsByEmail("player@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.create(request)).isInstanceOf(UserConflictException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void translatesConcurrentOracleUniqueViolation() {
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException(
                "internal", new SQLException("sensitive details", "23000", 1)));
        assertThatThrownBy(() -> service.create(request)).isInstanceOf(UserConflictException.class);
    }

    @Test
    void preservesOtherDatabaseFailures() {
        var failure = new DataIntegrityViolationException("internal",
                new SQLException("sensitive details", "23000", 1400));
        when(repository.saveAndFlush(any())).thenThrow(failure);
        assertThatThrownBy(() -> service.create(request)).isSameAs(failure);
    }
}
