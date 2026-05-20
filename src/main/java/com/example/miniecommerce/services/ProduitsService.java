package com.example.miniecommerce.services;

import com.example.miniecommerce.entities.Produits;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public interface ProduitsService {
    Produits saveProduits(Produits produits);
    Produits updateProduits(Produits produits);
    void deleteProduits(Long id);
    Produits getProduits(Long id);
    void deleteAllProduits();
    List<Produits> getAllProduits();
    List<Produits> getFilteredProducts(Long categoryId, String search);
}
