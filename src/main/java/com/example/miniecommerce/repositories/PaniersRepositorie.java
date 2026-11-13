package com.example.miniecommerce.repositories;

import com.example.miniecommerce.entities.Paniers;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaniersRepositorie extends JpaRepository<Paniers, Long> {
    Optional<Paniers> findByUtilisateurs_Id(Long utilisateurId);
}
