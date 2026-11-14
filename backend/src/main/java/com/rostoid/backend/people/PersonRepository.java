package com.rostoid.backend.people;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface PersonRepository extends JpaRepository<Person, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Person p where p.id = :id and p.ownerUsername = :owner")
    Optional<Person> findForUpdateOwned(@Param("id") Long id, @Param("owner") String owner);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Person p where p.id = :id")
    Optional<Person> findForUpdate(@Param("id") Long id);
    Optional<Person> findByIdAndOwnerUsername(Long id, String ownerUsername);
    List<Person> findAllByOwnerUsernameOrderByIdAsc(String ownerUsername);
    @Modifying
    @Query("update Person p set p.ownerUsername = :owner, p.registeredBy = coalesce(p.registeredBy, :owner) where p.ownerUsername is null")
    int claimLegacyRecords(@Param("owner") String owner);
}
