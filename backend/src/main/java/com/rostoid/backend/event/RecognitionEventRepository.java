package com.rostoid.backend.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecognitionEventRepository extends JpaRepository<RecognitionEvent, Long> {
    Optional<RecognitionEvent> findTopByPerson_IdAndCameraIdAndDirectionOrderByRecognizedAtDesc(
            Long personId, Long cameraId, CameraDirection direction);
    Optional<RecognitionEvent> findTopByPerson_IdOrderByRecognizedAtDesc(Long personId);
    List<RecognitionEvent> findAllByPerson_OwnerUsernameOrderByRecognizedAtDesc(String ownerUsername);
    List<RecognitionEvent> findAllByOrderByRecognizedAtDesc();
}
