package com.example.miniecommerce.services;

import com.example.miniecommerce.entities.LigneCommandes;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public interface LigneCommandesService {
    LigneCommandes saveLigneCommandes(LigneCommandes ligneCommandes);
    LigneCommandes updateLigneCommandes(LigneCommandes ligneCommandes);
    void deleteLigneCommandes(Long id);
    LigneCommandes getLigneCommandes(Long id);
    void deleteAllLigneCommandes();
    List<LigneCommandes> getAllLigneCommandes();
}
