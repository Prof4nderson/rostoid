package com.rostoid.backend.security;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class SecurityService {
    private static final int SETTINGS_ID = 1;
    private final SecuritySettingsRepository repository;
    private final SecureRandom random = new SecureRandom();

    public SecurityService(SecuritySettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void ensureInitialized() {
        if (repository.existsById(SETTINGS_ID)) return;
        byte[] encryption = new byte[32];
        byte[] auth = new byte[32];
        random.nextBytes(encryption);
        random.nextBytes(auth);
        repository.saveAndFlush(new SecuritySettings(SETTINGS_ID,
                Base64.getEncoder().encodeToString(encryption), Base64.getEncoder().encodeToString(auth)));
    }

    @Transactional(readOnly = true)
    public SecurityState state() {
        SecuritySettings settings = repository.findById(SETTINGS_ID)
                .orElseThrow(() -> new IllegalStateException("Configuração de segurança indisponível"));
        return new SecurityState(settings.getTokenHash() != null,
                settings.getEncryptionSalt(), settings.getAuthSalt());
    }

    @Transactional
    public boolean setup(String token) {
        SecuritySettings settings = repository.findForUpdate(SETTINGS_ID)
                .orElseThrow(() -> new IllegalStateException("Configuração de segurança indisponível"));
        if (settings.getTokenHash() != null) return false;
        settings.setTokenHash(hash(token));
        return true;
    }

    @Transactional(readOnly = true)
    public boolean authenticate(String token) {
        if (token == null) return false;
        return repository.findById(SETTINGS_ID).map(settings -> {
            if (settings.getTokenHash() == null) return false;
            byte[] expected = HexFormat.of().parseHex(settings.getTokenHash());
            byte[] supplied = HexFormat.of().parseHex(hash(token));
            return MessageDigest.isEqual(expected, supplied);
        }).orElse(false);
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    public record SecurityState(boolean configured, String encryptionSalt, String authSalt) {}
}
