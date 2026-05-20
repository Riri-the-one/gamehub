package com.example.miniecommerce.services.impl;

import com.example.miniecommerce.entities.Paniers;
import com.example.miniecommerce.entities.Utilisateurs;
import com.example.miniecommerce.entities.LignePaniers;
import com.example.miniecommerce.repositories.LignePaniersRepositorie;
import com.example.miniecommerce.repositories.PaniersRepositorie;
import com.example.miniecommerce.repositories.UtilisateursRepositorie;
import com.example.miniecommerce.services.PaniersService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@AllArgsConstructor
public class PaniersServiceImpl implements PaniersService {

    private PaniersRepositorie paniersRepositorie;
    private UtilisateursRepositorie utilisateursRepositorie;
    private LignePaniersRepositorie lignePaniersRepositorie;

    @Override
    public Paniers savePaniers(Paniers paniers) {
        return paniersRepositorie.save(paniers);
    }

    @Override
    public Paniers updatePaniers(Paniers paniers) {
        return paniersRepositorie.save(paniers);
    }

    @Override
    public void deletePaniers(Long id) {
        paniersRepositorie.deleteById(id);
    }

    @Override
    public Paniers getPaniers(Long id) {
        return paniersRepositorie.findById(id).orElse(null);
    }

    @Override
    public void deleteAllPaniers() {
        paniersRepositorie.deleteAll();
    }

    @Override
    public List<Paniers> getAllPaniers() {
        return paniersRepositorie.findAll();
    }

    @Override
    public Paniers getOrCreatePanierForUtilisateur(Long utilisateurId) {
        Utilisateurs user = utilisateursRepositorie.findById(utilisateurId).orElse(null);
        if (user == null) {
            return null;
        }

        Paniers panier = null;
        for (Paniers p : paniersRepositorie.findAll()) {
            if (p.getUtilisateurs() != null && p.getUtilisateurs().getId().equals(utilisateurId)) {
                panier = p;
                break;
            }
        }

        if (panier == null) {
            panier = new Paniers();
            panier.setDate(new Date());
            panier.setUtilisateurs(user);
            panier.setLignePaniers(new ArrayList<>());
            panier = paniersRepositorie.save(panier);
        }

        return panier;
    }

    @Override
    public Double calculateTotal(Long panierId) {
        Paniers panier = paniersRepositorie.findById(panierId).orElse(null);
        if (panier == null || panier.getLignePaniers() == null) {
            return 0.0;
        }

        double total = 0.0;
        for (LignePaniers lp : panier.getLignePaniers()) {
            if (lp.getProduits() != null && lp.getProduits().getPrix() != null) {
                total += lp.getProduits().getPrix() * lp.getQuantite();
            }
        }
        return total;
    }

    @Override
    public void clearPanier(Long panierId) {
        Paniers panier = paniersRepositorie.findById(panierId).orElse(null);
        if (panier != null) {
            if (panier.getLignePaniers() != null && !panier.getLignePaniers().isEmpty()) {
                lignePaniersRepositorie.deleteAll(panier.getLignePaniers());
                panier.getLignePaniers().clear();
            }
            paniersRepositorie.save(panier);
        }
    }
}
