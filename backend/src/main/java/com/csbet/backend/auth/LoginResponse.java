package com.csbet.backend.auth;
public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
    @Override public String toString() { return "LoginResponse[redacted]"; }
}
