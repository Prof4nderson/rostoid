package com.rostoid.backend.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "settings")
public class SecuritySettings {
    @Id
    private Integer id;

    @Column(nullable = false, length = 64)
    private String encryptionSalt;

    @Column(nullable = false, length = 64)
    private String authSalt;

    @Column(length = 64)
    private String tokenHash;

    @Column(length = 128)
    private String jwtSecret;

    protected SecuritySettings() {}

    public SecuritySettings(Integer id, String encryptionSalt, String authSalt, String jwtSecret) {
        this.id = id;
        this.encryptionSalt = encryptionSalt;
        this.authSalt = authSalt;
        this.jwtSecret = jwtSecret;
    }

    public Integer getId() { return id; }
    public String getEncryptionSalt() { return encryptionSalt; }
    public String getAuthSalt() { return authSalt; }
    public String getTokenHash() { return tokenHash; }
    public String getJwtSecret() { return jwtSecret; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }
}
