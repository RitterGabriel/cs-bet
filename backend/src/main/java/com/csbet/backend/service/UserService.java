package com.csbet.backend.service;

import com.csbet.backend.dto.CreateUserRequest;
import com.csbet.backend.dto.UserResponse;
import com.csbet.backend.entity.User;
import com.csbet.backend.exception.UserConflictException;
import com.csbet.backend.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import org.springframework.security.authentication.BadCredentialsException;
import java.sql.SQLException;
import java.time.LocalDateTime;

@Service
public class UserService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse findAuthenticated(Long userId) {
        return repository.findById(userId).map(UserResponse::from)
                .orElseThrow(() -> new BadCredentialsException("Não autenticado."));
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (repository.existsByUsername(request.username())) {
            throw new UserConflictException("Nome de usuário já cadastrado.");
        }
        if (repository.existsByEmail(request.email())) {
            throw new UserConflictException("E-mail já cadastrado.");
        }
        User user = new User(request.username(), request.email(),
                passwordEncoder.encode(request.password()), BigDecimal.ZERO, LocalDateTime.now());
        try {
            return UserResponse.from(repository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            // ORA-00001: a restrição UNIQUE detectou um cadastro concorrente.
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof SQLException sqlException && sqlException.getErrorCode() == 1) {
                    throw new UserConflictException("Nome de usuário ou e-mail já cadastrado.");
                }
            }
            throw exception;
        }
    }
}
