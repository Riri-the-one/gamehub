package com.example.miniecommerce.services;

import com.example.miniecommerce.entities.LignePaniers;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public interface LignePaniersService {
    LignePaniers saveLignePaniers(LignePaniers lignePaniers);
    LignePaniers updateLignePaniers(LignePaniers lignePaniers);
    void deleteLignePaniers(Long id);
    LignePaniers getLignePaniers(Long id);
    void deleteAllLignePaniers();
    List<LignePaniers> getAllLignePaniers();
    LignePaniers addProduitToPanier(Long panierId, Long produitId, Integer quantite);
}
