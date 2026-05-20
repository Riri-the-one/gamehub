package com.example.miniecommerce.services.impl;

import com.example.miniecommerce.entities.Categories;
import com.example.miniecommerce.repositories.CategoriesRepositorie;
import com.example.miniecommerce.services.CategoriesService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@AllArgsConstructor
public class CategoriesServiceImpl implements CategoriesService {

    private CategoriesRepositorie categoriesRepositorie;

    @Override
    public Categories saveCategories(Categories categories) {
        return categoriesRepositorie.save(categories);
    }

    @Override
    public Categories updateCategories(Categories categories) {
        return categoriesRepositorie.save(categories);
    }

    @Override
    public void deleteCategories(Long id) {
        categoriesRepositorie.deleteById(id);
    }

    @Override
    public Categories getCategories(Long id) {
        return categoriesRepositorie.findById(id).orElse(null);
    }

    @Override
    public void deleteAllCategories() {
        categoriesRepositorie.deleteAll();
    }

    @Override
    public List<Categories> getAllCategories() {
        return categoriesRepositorie.findAll();
    }
}
