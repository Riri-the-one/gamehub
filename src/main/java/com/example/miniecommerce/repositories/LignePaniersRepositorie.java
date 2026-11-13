package com.example.miniecommerce.repositories;

import com.example.miniecommerce.entities.LignePaniers;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LignePaniersRepositorie extends JpaRepository<LignePaniers, Long> {
    Optional<LignePaniers> findByIdAndPaniers_Id(Long id, Long panierId);
    Optional<LignePaniers> findByPaniers_IdAndProduits_Id(Long panierId, Long produitId);
    List<LignePaniers> findByPaniers_Id(Long panierId);
    boolean existsByProduits_Id(Long produitId);
}
