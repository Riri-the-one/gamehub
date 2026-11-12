package com.example.miniecommerce.services.impl;

import com.example.miniecommerce.entities.Produits;
import com.example.miniecommerce.repositories.CategoriesRepositorie;
import com.example.miniecommerce.repositories.LigneCommandesRepositorie;
import com.example.miniecommerce.repositories.LignePaniersRepositorie;
import com.example.miniecommerce.repositories.ProduitsRepositorie;
import com.example.miniecommerce.services.Money;
import com.example.miniecommerce.services.ProduitsService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class ProduitsServiceImpl implements ProduitsService {

    private ProduitsRepositorie produitsRepositorie;
    private CategoriesRepositorie categories;
    private LignePaniersRepositorie cartLines;
    private LigneCommandesRepositorie orderLines;
    private EntityManager entityManager;

    private void validate(Produits product) {
        if(product.getNom()==null || product.getNom().isBlank() || product.getNom().length()>255
                || (product.getDescription()!=null && product.getDescription().length()>255)
                || product.getQuantite()==null || product.getQuantite()<0 || product.getQuantite()>1000000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Produit invalide.");
        }
        product.setPrix(Money.price(product.getPrix()));
        if(product.getImage()!=null && !product.getImage().isBlank()
                && !product.getImage().matches("(?i)[a-z0-9][a-z0-9._-]{0,240}\\.(png|jpe?g|webp|gif|svg)")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Nom de fichier image invalide.");
        }
        if(product.getImage()!=null && product.getImage().isBlank()) product.setImage(null);
        if(product.getCategorie()!=null && product.getCategorie().getId()!=null) {
            product.setCategorie(categories.findById(product.getCategorie().getId()).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.BAD_REQUEST,"Catégorie inconnue.")));
        } else product.setCategorie(null);
    }

    @Override
    @Transactional
    public Produits saveProduits(Produits produits) {
        if(produits.getId()!=null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        validate(produits);
        return produitsRepositorie.save(produits);
    }

    @Override
    @Transactional
    public Produits updateProduits(Produits produits) {
        validate(produits);
        var current=produitsRepositorie.findForUpdate(produits.getId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND));
        entityManager.refresh(current,LockModeType.PESSIMISTIC_WRITE);
        if(produits.getVersion()==null || !produits.getVersion().equals(current.getVersion())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce produit a changé. Rechargez le formulaire avant de réessayer.");
        }
        current.setNom(produits.getNom());current.setDescription(produits.getDescription());current.setPrix(produits.getPrix());
        current.setQuantite(produits.getQuantite());current.setImage(produits.getImage());current.setCategorie(produits.getCategorie());
        return current;
    }

    @Override
    @Transactional
    public void deleteProduits(Long id) {
        produitsRepositorie.findForUpdate(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(cartLines.existsByProduits_Id(id) || orderLines.existsByProduit_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce produit est référencé par un panier ou une commande.");
        }
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
