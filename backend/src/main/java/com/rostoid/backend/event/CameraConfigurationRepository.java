package com.rostoid.backend.event;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CameraConfigurationRepository extends JpaRepository<CameraConfiguration, Long> {
    List<CameraConfiguration> findAllByOrderByIdAsc();
    Optional<CameraConfiguration> findByDeviceId(String deviceId);
}
