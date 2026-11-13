package com.rostoid.backend.event;

import com.rostoid.backend.people.Person;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "recognition_events")
public class RecognitionEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(nullable = false)
    private Instant recognizedAt;

    // Snapshots intentionally have no FK to removable camera configuration rows.
    @Column(name = "camera_id")
    private Long cameraId;

    @Column(name = "camera_label", length = 255)
    private String cameraLabel;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private CameraDirection direction;

    protected RecognitionEvent() {}

    public RecognitionEvent(Person person, Instant recognizedAt) {
        this(person, recognizedAt, null, null, null);
    }

    public RecognitionEvent(Person person, Instant recognizedAt, Long cameraId,
                            String cameraLabel, CameraDirection direction) {
        this.person = person;
        this.recognizedAt = recognizedAt;
        this.cameraId = cameraId;
        this.cameraLabel = cameraLabel;
        this.direction = direction;
    }

    public Long getId() { return id; }
    public Person getPerson() { return person; }
    public Instant getRecognizedAt() { return recognizedAt; }
    public Long getCameraId() { return cameraId; }
    public String getCameraLabel() { return cameraLabel; }
    public CameraDirection getDirection() { return direction; }
}
