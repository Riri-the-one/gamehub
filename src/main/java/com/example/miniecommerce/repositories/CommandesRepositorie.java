package com.example.miniecommerce.repositories;

import com.example.miniecommerce.entities.Commandes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommandesRepositorie extends JpaRepository<Commandes, Long> {
}
