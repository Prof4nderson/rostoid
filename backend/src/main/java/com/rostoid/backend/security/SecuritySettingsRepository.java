package com.rostoid.backend.security;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SecuritySettingsRepository extends JpaRepository<SecuritySettings, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SecuritySettings s where s.id = :id")
    Optional<SecuritySettings> findForUpdate(@Param("id") Integer id);
}
