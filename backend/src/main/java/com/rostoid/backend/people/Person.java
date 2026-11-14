package com.rostoid.backend.people;

import com.rostoid.backend.event.RecognitionEvent;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Entity
@Table(name = "people")
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 32)
    private String publicId;

    @Lob
    @Column(nullable = false)
    private String faceCiphertext;

    @Column(nullable = false, length = 512)
    private String faceIv;

    @Lob
    private String profileCiphertext;

    @Column(length = 512)
    private String profileIv;

    @Column(nullable = false)
    private Instant consentAt;

    @Column(nullable = false)
    private Instant createdAt;
    @Column(name = "registered_by", length = 80)
    private String registeredBy;

    @OneToMany(mappedBy = "person", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RecognitionEvent> events = new ArrayList<>();

    protected Person() {}

    public Person(String faceCiphertext, String faceIv, Instant now) { this(faceCiphertext, faceIv, now, null); }

    public Person(String faceCiphertext, String faceIv, Instant now, String registeredBy) {
        this.faceCiphertext = faceCiphertext;
        this.faceIv = faceIv;
        this.consentAt = now;
        this.createdAt = now;
        this.registeredBy = registeredBy;
    }

    public void assignPublicId() {
        this.publicId = String.format(Locale.ROOT, "RID-%06d", id);
    }

    public void updateProfile(String ciphertext, String iv) {
        this.profileCiphertext = ciphertext;
        this.profileIv = iv;
    }

    public Long getId() { return id; }
    public String getPublicId() { return publicId; }
    public String getFaceCiphertext() { return faceCiphertext; }
    public String getFaceIv() { return faceIv; }
    public String getProfileCiphertext() { return profileCiphertext; }
    public String getProfileIv() { return profileIv; }
    public Instant getConsentAt() { return consentAt; }
    public Instant getCreatedAt() { return createdAt; }
    public String getRegisteredBy() { return registeredBy; }
}
