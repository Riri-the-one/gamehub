package com.example.miniecommerce.services;

import com.example.miniecommerce.entities.LigneCommandes;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public interface LigneCommandesService {
    LigneCommandes saveLigneCommandes(LigneCommandes ligneCommandes);
    LigneCommandes updateLigneCommandes(LigneCommandes ligneCommandes);
    void deleteLigneCommandes(Long id);
    LigneCommandes getLigneCommandes(Long id);
    void deleteAllLigneCommandes();
    List<LigneCommandes> getAllLigneCommandes();
}
