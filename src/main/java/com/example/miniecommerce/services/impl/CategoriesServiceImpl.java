package com.example.miniecommerce.services.impl;

import com.example.miniecommerce.entities.Categories;
import com.example.miniecommerce.repositories.CategoriesRepositorie;
import com.example.miniecommerce.repositories.ProduitsRepositorie;
import com.example.miniecommerce.services.CategoriesService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class CategoriesServiceImpl implements CategoriesService {

    private CategoriesRepositorie categoriesRepositorie;
    private ProduitsRepositorie products;

    private void validate(Categories category) {
        if(category.getNom()==null || category.getNom().isBlank() || category.getNom().length()>255
                || (category.getDescription()!=null && category.getDescription().length()>255)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Catégorie invalide.");
        }
    }

    @Override
    public Categories saveCategories(Categories categories) {
        validate(categories);
        return categoriesRepositorie.save(categories);
    }

    @Override
    public Categories updateCategories(Categories categories) {
        validate(categories);
        return categoriesRepositorie.save(categories);
    }

    @Override
    public void deleteCategories(Long id) {
        if(products.existsByCategorie_Id(id)) throw new ResponseStatusException(
                HttpStatus.CONFLICT,"Cette catégorie contient encore des produits.");
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
