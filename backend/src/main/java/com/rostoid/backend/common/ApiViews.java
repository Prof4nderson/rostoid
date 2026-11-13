package com.rostoid.backend.common;

import com.rostoid.backend.event.CameraConfiguration;
import com.rostoid.backend.event.CameraDirection;
import com.rostoid.backend.event.RecognitionEvent;
import com.rostoid.backend.people.Person;

import java.time.Instant;

public final class ApiViews {
    private ApiViews() {}

    public record PersonView(Long id, String publicId, String faceCiphertext, String faceIv,
                             String profileCiphertext, String profileIv, Instant consentAt,
                             Instant createdAt, Instant lastRecognizedAt) {
        public static PersonView from(Person p, Instant lastRecognizedAt) {
            return new PersonView(p.getId(), p.getPublicId(), p.getFaceCiphertext(), p.getFaceIv(),
                    p.getProfileCiphertext(), p.getProfileIv(), p.getConsentAt(), p.getCreatedAt(), lastRecognizedAt);
        }
    }

    public record CameraView(Long id, String deviceId, String label, CameraDirection direction, Instant updatedAt) {
        public static CameraView from(CameraConfiguration camera) {
            return new CameraView(camera.getId(), camera.getDeviceId(), camera.getLabel(),
                    camera.getDirection(), camera.getUpdatedAt());
        }
    }

    public record EventView(Long id, Long personId, String publicId, Instant recognizedAt,
                            Long cameraId, String cameraLabel, CameraDirection direction) {
        public static EventView from(RecognitionEvent e) {
            return new EventView(e.getId(), e.getPerson().getId(), e.getPerson().getPublicId(),
                    e.getRecognizedAt(), e.getCameraId(), e.getCameraLabel(), e.getDirection());
        }
    }

    public record CreatedEventView(Long id, Long personId, String publicId, Instant recognizedAt,
                                   boolean created, Long cameraId, String cameraLabel, CameraDirection direction) {
        public static CreatedEventView from(RecognitionEvent e, boolean created) {
            return new CreatedEventView(e.getId(), e.getPerson().getId(), e.getPerson().getPublicId(),
                    e.getRecognizedAt(), created, e.getCameraId(), e.getCameraLabel(), e.getDirection());
        }
    }
}
