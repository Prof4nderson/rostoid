package com.rostoid.backend.security;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import com.rostoid.backend.people.PersonRepository;
import com.rostoid.backend.event.CameraConfigurationRepository;

@Service
public class SecurityService {
    private static final int SETTINGS_ID = 1;
    private final SecuritySettingsRepository settings;
    private final UserAccountRepository users;
    private final PersonRepository people;
    private final CameraConfigurationRepository cameras;
    private final SecureRandom random = new SecureRandom();
    public SecurityService(SecuritySettingsRepository settings, UserAccountRepository users, PersonRepository people, CameraConfigurationRepository cameras) { this.settings = settings; this.users = users; this.people = people; this.cameras = cameras; }
    @Transactional public void ensureInitialized() {
        if (settings.existsById(SETTINGS_ID)) {
            SecuritySettings existing = settings.findById(SETTINGS_ID).orElseThrow();
            if (existing.getJwtSecret() == null || existing.getJwtSecret().isBlank()) { byte[] jwt = new byte[48]; random.nextBytes(jwt); existing.setJwtSecret(b64(jwt)); settings.saveAndFlush(existing); }
            claimLegacyData();
            return;
        }
        byte[] encryption = new byte[32], auth = new byte[32], jwt = new byte[48];
        random.nextBytes(encryption); random.nextBytes(auth); random.nextBytes(jwt);
        settings.saveAndFlush(new SecuritySettings(SETTINGS_ID, b64(encryption), b64(auth), b64(jwt)));
    }
    private void claimLegacyData() { if (users.count() > 0) { String owner = users.findAll().getFirst().getUsername(); people.claimLegacyRecords(owner); cameras.claimLegacyRecords(owner); } }
    @Transactional(readOnly = true) public SecurityState state() {
        SecuritySettings s = settings.findById(SETTINGS_ID).orElseThrow(() -> new IllegalStateException("Configuração indisponível"));
        return new SecurityState(users.count() > 0, s.getEncryptionSalt(), s.getAuthSalt());
    }
    @Transactional public AuthResult register(String username, String password) {
        if (users.count() > 0) throw new IllegalArgumentException("O cadastro inicial já foi concluído");
        String normalized = normalize(username);
        if (users.existsByUsernameIgnoreCase(normalized)) throw new IllegalArgumentException("Usuário já cadastrado");
        UserAccount user = users.save(new UserAccount(normalized, hashPassword(password)));
        return new AuthResult(user.getUsername(), issue(user.getUsername()));
    }
    @Transactional(readOnly = true) public List<UserAccountView> listUsers() {
        String current = AuthenticatedUser.username();
        return users.findAll().stream().map(u -> new UserAccountView(u.getId(), u.getUsername(), u.getCreatedAt(), u.getUsername().equalsIgnoreCase(current))).toList();
    }
    @Transactional public UserAccountView createUser(String username, String password) {
        String normalized = normalize(username);
        if (users.existsByUsernameIgnoreCase(normalized)) throw new IllegalArgumentException("Usuário já cadastrado");
        UserAccount user = users.saveAndFlush(new UserAccount(normalized, hashPassword(password)));
        return new UserAccountView(user.getId(), user.getUsername(), user.getCreatedAt(), false);
    }
    @Transactional public UserAccountView updateUser(Long id, String username, String password) {
        UserAccount user = users.findById(id).orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        if (username != null && !username.isBlank()) {
            String normalized = normalize(username);
            if (user.getUsername().equalsIgnoreCase(AuthenticatedUser.username()) && !normalized.equalsIgnoreCase(user.getUsername())) throw new IllegalArgumentException("Altere o usuário atual encerrando a sessão e entrando novamente");
            if (!normalized.equalsIgnoreCase(user.getUsername()) && users.existsByUsernameIgnoreCase(normalized)) throw new IllegalArgumentException("Usuário já cadastrado");
            user.setUsername(normalized);
        }
        if (password != null && !password.isBlank()) { if (password.length() < 12) throw new IllegalArgumentException("A senha deve ter pelo menos 12 caracteres"); user.setPasswordHash(hashPassword(password)); }
        users.saveAndFlush(user);
        return new UserAccountView(user.getId(), user.getUsername(), user.getCreatedAt(), user.getUsername().equalsIgnoreCase(AuthenticatedUser.username()));
    }
    @Transactional public void deleteUser(Long id) {
        UserAccount user = users.findById(id).orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        if (user.getUsername().equalsIgnoreCase(AuthenticatedUser.username())) throw new IllegalArgumentException("Não é possível excluir o usuário da sessão atual");
        users.delete(user);
    }
    @Transactional(readOnly = true) public AuthResult login(String username, String password) {
        UserAccount user = users.findByUsernameIgnoreCase(normalize(username)).orElseThrow(() -> new IllegalArgumentException("Usuário ou senha inválidos"));
        if (!verifyPassword(password, user.getPasswordHash())) throw new IllegalArgumentException("Usuário ou senha inválidos");
        return new AuthResult(user.getUsername(), issue(user.getUsername()));
    }
    @Transactional(readOnly = true) public String authenticate(String token) {
        try {
            String[] p = token == null ? new String[0] : token.split("\\.");
            if (p.length != 3) return null;
            if (!MessageDigest.isEqual(hmac(p[0] + "." + p[1]).getBytes(StandardCharsets.US_ASCII), p[2].getBytes(StandardCharsets.US_ASCII))) return null;
            String payload = new String(Base64.getUrlDecoder().decode(p[1]), StandardCharsets.UTF_8);
            String sub = payload.replaceAll(".*\\\"sub\\\":\\\"([^\\\"]+)\\\".*", "$1");
            long exp = Long.parseLong(payload.replaceAll(".*\\\"exp\\\":(\\d+).*", "$1"));
            return exp > Instant.now().getEpochSecond() && users.existsByUsernameIgnoreCase(sub) ? sub : null;
        } catch (Exception ignored) { return null; }
    }
    private String issue(String username) { long now = Instant.now().getEpochSecond(); String h = enc("{\"alg\":\"HS256\",\"typ\":\"JWT\"}"); String p = enc("{\"sub\":\"" + username + "\",\"iat\":" + now + ",\"exp\":" + (now + 28800) + "}"); return h + "." + p + "." + hmac(h + "." + p); }
    private String hmac(String value) { try { Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new javax.crypto.spec.SecretKeySpec(secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256")); return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception e) { throw new IllegalStateException(e); } }
    private String secret() { return settings.findById(SETTINGS_ID).orElseThrow().getJwtSecret(); }
    private String hashPassword(String password) { try { byte[] salt = new byte[16]; random.nextBytes(salt); return "pbkdf2$" + b64(salt) + "$" + b64(pbkdf2(password, salt)); } catch (Exception e) { throw new IllegalStateException(e); } }
    private boolean verifyPassword(String password, String stored) { try { String[] p = stored.split("\\$"); return p.length == 3 && MessageDigest.isEqual(pbkdf2(password, Base64.getDecoder().decode(p[1])), Base64.getDecoder().decode(p[2])); } catch (Exception e) { return false; } }
    private byte[] pbkdf2(String password, byte[] salt) throws Exception { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(new PBEKeySpec(password.toCharArray(), salt, 210000, 256)).getEncoded(); }
    private static String normalize(String username) { String value = username == null ? "" : username.trim().toLowerCase(); if (!value.matches("[a-z0-9._-]{3,80}")) throw new IllegalArgumentException("Usuário deve ter 3 a 80 caracteres: letras, números, ponto, hífen ou sublinhado"); return value; }
    private static String b64(byte[] value) { return Base64.getEncoder().encodeToString(value); }
    private static String enc(String value) { return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8)); }
    public record SecurityState(boolean configured, String encryptionSalt, String authSalt) {}
    public record AuthResult(String username, String token) {}
}
