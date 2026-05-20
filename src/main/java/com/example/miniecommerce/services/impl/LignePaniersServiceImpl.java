package com.example.miniecommerce.services.impl;

import com.example.miniecommerce.entities.LignePaniers;
import com.example.miniecommerce.entities.Paniers;
import com.example.miniecommerce.entities.Produits;
import com.example.miniecommerce.repositories.LignePaniersRepositorie;
import com.example.miniecommerce.repositories.PaniersRepositorie;
import com.example.miniecommerce.repositories.ProduitsRepositorie;
import com.example.miniecommerce.services.LignePaniersService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class LignePaniersServiceImpl implements LignePaniersService {

    private LignePaniersRepositorie lignePaniersRepositorie;
    private PaniersRepositorie paniersRepositorie;
    private ProduitsRepositorie produitsRepositorie;

    @Override
    public LignePaniers saveLignePaniers(LignePaniers lignePaniers) {
        return lignePaniersRepositorie.save(lignePaniers);
    }

    @Override
    public LignePaniers updateLignePaniers(LignePaniers lignePaniers) {
        return lignePaniersRepositorie.save(lignePaniers);
    }

    @Override
    public void deleteLignePaniers(Long id) {
        lignePaniersRepositorie.deleteById(id);
    }

    @Override
    public LignePaniers getLignePaniers(Long id) {
        return lignePaniersRepositorie.findById(id).orElse(null);
    }

    @Override
    public void deleteAllLignePaniers() {
        lignePaniersRepositorie.deleteAll();
    }

    @Override
    public List<LignePaniers> getAllLignePaniers() {
        return lignePaniersRepositorie.findAll();
    }

    @Override
    public LignePaniers addProduitToPanier(Long panierId, Long produitId, Integer quantite) {
        Paniers panier = paniersRepositorie.findById(panierId).orElse(null);
        Produits produit = produitsRepositorie.findById(produitId).orElse(null);

        if (panier == null || produit == null) {
            return null;
        }

        LignePaniers ligneExistante = null;
        for (LignePaniers lp : lignePaniersRepositorie.findAll()) {
            if (lp.getPaniers() != null && lp.getPaniers().getId().equals(panierId)
                    && lp.getProduits() != null && lp.getProduits().getId().equals(produitId)) {
                ligneExistante = lp;
                break;
            }
        }

        if (ligneExistante != null) {
            ligneExistante.setQuantite(ligneExistante.getQuantite() + quantite);
            return lignePaniersRepositorie.save(ligneExistante);
        }

        else {
            LignePaniers nouvelleLigne = new LignePaniers();
            nouvelleLigne.setPaniers(panier);
            nouvelleLigne.setProduits(produit);
            nouvelleLigne.setQuantite(quantite);
            return lignePaniersRepositorie.save(nouvelleLigne);
        }
    }
}
