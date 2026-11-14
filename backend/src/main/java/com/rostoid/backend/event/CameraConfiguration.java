package com.rostoid.backend.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "camera_configurations")
public class CameraConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 512)
    private String deviceId;

    @Column(nullable = false, length = 255)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CameraDirection direction;

    @Column(nullable = false)
    private Instant updatedAt;
    @Column(name = "owner_username", length = 80)
    private String ownerUsername;

    protected CameraConfiguration() {}

    public CameraConfiguration(String deviceId, String label, CameraDirection direction, Instant updatedAt) { this(deviceId, label, direction, updatedAt, null); }
    public CameraConfiguration(String deviceId, String label, CameraDirection direction, Instant updatedAt, String ownerUsername) {
        this.deviceId = deviceId;
        this.label = label;
        this.direction = direction;
        this.updatedAt = updatedAt;
        this.ownerUsername = ownerUsername;
    }

    public void update(String label, CameraDirection direction, Instant updatedAt) {
        this.label = label;
        this.direction = direction;
        this.updatedAt = updatedAt;
        this.ownerUsername = ownerUsername;
    }

    public Long getId() { return id; }
    public String getDeviceId() { return deviceId; }
    public String getLabel() { return label; }
    public CameraDirection getDirection() { return direction; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getOwnerUsername() { return ownerUsername; }
}
