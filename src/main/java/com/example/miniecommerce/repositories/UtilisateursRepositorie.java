package com.example.miniecommerce.repositories;

import com.example.miniecommerce.entities.Utilisateurs;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UtilisateursRepositorie extends JpaRepository<Utilisateurs, Long> {
    Optional<Utilisateurs> findByGuestTokenHash(String hash);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Utilisateurs u where u.id = :id")
    Optional<Utilisateurs> findLockedById(@Param("id") Long id);
}
