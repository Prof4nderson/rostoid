package com.rostoid.backend.event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface CameraConfigurationRepository extends JpaRepository<CameraConfiguration, Long> {
    List<CameraConfiguration> findAllByOwnerUsernameOrderByIdAsc(String ownerUsername);
    List<CameraConfiguration> findAllByOwnerUsername(String ownerUsername);
    Optional<CameraConfiguration> findByOwnerUsernameAndDeviceId(String ownerUsername, String deviceId);
    Optional<CameraConfiguration> findByIdAndOwnerUsername(Long id, String ownerUsername);
    @Modifying
    @Query("update CameraConfiguration c set c.ownerUsername = :owner where c.ownerUsername is null")
    int claimLegacyRecords(@Param("owner") String owner);
}
