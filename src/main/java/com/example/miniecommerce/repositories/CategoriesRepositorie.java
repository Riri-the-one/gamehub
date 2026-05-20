package com.example.miniecommerce.repositories;

import com.example.miniecommerce.entities.Categories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoriesRepositorie extends JpaRepository<Categories, Long> {
}
