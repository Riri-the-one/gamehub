package com.example.miniecommerce.services;

import com.example.miniecommerce.entities.Paniers;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public interface PaniersService {
    Paniers savePaniers(Paniers paniers);
    Paniers updatePaniers(Paniers paniers);
    void deletePaniers(Long id);
    Paniers getPaniers(Long id);
    void deleteAllPaniers();
    List<Paniers> getAllPaniers();
    Paniers getOrCreatePanierForUtilisateur(Long utilisateurId);
    BigDecimal calculateTotal(Long panierId);
    void clearPanier(Long panierId);
    void addForUtilisateur(Long utilisateurId, Long produitId, Integer quantite);
    void removeForUtilisateur(Long utilisateurId, Long ligneId);
    void clearForUtilisateur(Long utilisateurId);
}
