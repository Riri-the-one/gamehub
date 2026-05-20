package com.example.miniecommerce.services.impl;

import com.example.miniecommerce.entities.LigneCommandes;
import com.example.miniecommerce.repositories.LigneCommandesRepositorie;
import com.example.miniecommerce.services.LigneCommandesService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@AllArgsConstructor
public class LigneCommandesServiceImpl implements LigneCommandesService {

    private LigneCommandesRepositorie ligneCommandesRepositorie;

    @Override
    public LigneCommandes saveLigneCommandes(LigneCommandes ligneCommandes) {
        return ligneCommandesRepositorie.save(ligneCommandes);
    }

    @Override
    public LigneCommandes updateLigneCommandes(LigneCommandes ligneCommandes) {
        return ligneCommandesRepositorie.save(ligneCommandes);
    }

    @Override
    public void deleteLigneCommandes(Long id) {
        ligneCommandesRepositorie.deleteById(id);
    }

    @Override
    public LigneCommandes getLigneCommandes(Long id) {
        return ligneCommandesRepositorie.findById(id).orElse(null);
    }

    @Override
    public void deleteAllLigneCommandes() {
        ligneCommandesRepositorie.deleteAll();
    }

    @Override
    public List<LigneCommandes> getAllLigneCommandes() {
        return ligneCommandesRepositorie.findAll();
    }
}
