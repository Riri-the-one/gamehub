package com.example.miniecommerce.services.impl;

import com.example.miniecommerce.entities.Produits;
import com.example.miniecommerce.repositories.ProduitsRepositorie;
import com.example.miniecommerce.services.ProduitsService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class ProduitsServiceImpl implements ProduitsService {

    private ProduitsRepositorie produitsRepositorie;

    @Override
    public Produits saveProduits(Produits produits) {
        return produitsRepositorie.save(produits);
    }

    @Override
    public Produits updateProduits(Produits produits) {
        return produitsRepositorie.save(produits);
    }

    @Override
    public void deleteProduits(Long id) {
        produitsRepositorie.deleteById(id);
    }

    @Override
    public Produits getProduits(Long id) {
        return produitsRepositorie.findById(id).orElse(null);
    }

    @Override
    public void deleteAllProduits() {
        produitsRepositorie.deleteAll();
    }

    @Override
    public List<Produits> getAllProduits() {
        return produitsRepositorie.findAll();
    }

    @Override
    public List<Produits> getFilteredProducts(Long categoryId, String search) {
        List<Produits> tous = produitsRepositorie.findAll();
        
        if (search != null && !search.trim().isEmpty()) {
            List<Produits> resultat = new ArrayList<>();
            for (Produits p : tous) {
                if (p.getNom() != null && p.getNom().toLowerCase().contains(search.toLowerCase())) {
                    resultat.add(p);
                }
            }
            return resultat;
        } 
        else if (categoryId != null) {
            List<Produits> resultat = new ArrayList<>();
            for (Produits p : tous) {
                if (p.getCategorie() != null && p.getCategorie().getId().equals(categoryId)) {
                    resultat.add(p);
                }
            }
            return resultat;
        } 
        else {
            return tous;
        }
    }
}
