package com.example.miniecommerce.services;

import com.example.miniecommerce.entities.Categories;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public interface CategoriesService {
    Categories saveCategories(Categories categories);
    Categories updateCategories(Categories categories);
    void deleteCategories(Long id);
    Categories getCategories(Long id);
    void deleteAllCategories();
    List<Categories> getAllCategories();
}
