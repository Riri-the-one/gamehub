package com.example.miniecommerce.repositories;

import com.example.miniecommerce.entities.Commandes;
import com.example.miniecommerce.enums.CommandeEnum;
import jakarta.persistence.LockModeType;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommandesRepositorie extends JpaRepository<Commandes, Long> {
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update Commandes c set c.commandestatut = :next where c.id = :id and c.utilisateur.id = :owner and c.commandestatut = :expected")
    int transition(@org.springframework.data.repository.query.Param("id") Long id,
        @org.springframework.data.repository.query.Param("owner") Long owner,
        @org.springframework.data.repository.query.Param("expected") com.example.miniecommerce.enums.CommandeEnum expected,
        @org.springframework.data.repository.query.Param("next") com.example.miniecommerce.enums.CommandeEnum next);
    Optional<Commandes> findByCheckoutKey(String hash);
    List<Commandes> findTop100ByCommandestatutAndDateCommandeBeforeAndCheckoutKeyIsNotNull(
        CommandeEnum state, Date cutoff);
    Optional<Commandes> findByIdAndUtilisateur_Id(Long id, Long utilisateurId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Commandes c where c.id = :id and c.utilisateur.id = :owner")
    Optional<Commandes> findOwnedForUpdate(
        @Param("id") Long id,
        @Param("owner") Long owner);
}
