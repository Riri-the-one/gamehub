package com.example.miniecommerce.repositories;

import com.example.miniecommerce.entities.Produits;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProduitsRepositorie extends JpaRepository<Produits, Long> {
    boolean existsByCategorie_Id(Long categorieId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Produits p where p.id = :id")
    Optional<Produits> findForUpdate(@Param("id") Long id);

    @Modifying
    @Query("update Produits p set p.quantite = p.quantite - :quantity, p.version = p.version + 1 where p.id = :id and p.quantite >= :quantity")
    int decrementAvailableStock(@Param("id") Long id,
        @Param("quantity") int quantity);
}
