package com.example.miniecommerce.services;

import com.example.miniecommerce.entities.Categories;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public interface CategoriesService {
    Categories saveCategories(Categories categories);
    Categories updateCategories(Categories categories);
    void deleteCategories(Long id);
    Categories getCategories(Long id);
    void deleteAllCategories();
    List<Categories> getAllCategories();
}
