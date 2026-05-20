package com.example.miniecommerce.repositories;

import com.example.miniecommerce.entities.Paiements;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaiementsRepositorie extends JpaRepository<Paiements, Long> {
}
