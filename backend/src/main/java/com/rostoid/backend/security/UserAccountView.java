package com.rostoid.backend.security;
import java.time.Instant;
public record UserAccountView(Long id, String username, Instant createdAt, boolean current) {}
