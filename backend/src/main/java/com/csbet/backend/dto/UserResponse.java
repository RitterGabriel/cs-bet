package com.csbet.backend.dto;

import com.csbet.backend.entity.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserResponse(Long id, String username, String email,
                           BigDecimal balance, LocalDateTime createdAt) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(),
                user.getBalance(), user.getCreatedAt());
    }
}
