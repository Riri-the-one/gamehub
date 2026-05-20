package com.example.miniecommerce.repositories;

import com.example.miniecommerce.entities.Utilisateurs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UtilisateursRepositorie extends JpaRepository<Utilisateurs, Long> {
}
