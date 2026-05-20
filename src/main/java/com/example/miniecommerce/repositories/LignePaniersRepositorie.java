package com.example.miniecommerce.repositories;

import com.example.miniecommerce.entities.LignePaniers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LignePaniersRepositorie extends JpaRepository<LignePaniers, Long> {
}
