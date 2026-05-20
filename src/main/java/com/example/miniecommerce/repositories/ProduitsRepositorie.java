package com.example.miniecommerce.repositories;

import com.example.miniecommerce.entities.Produits;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProduitsRepositorie extends JpaRepository<Produits, Long> {
}
