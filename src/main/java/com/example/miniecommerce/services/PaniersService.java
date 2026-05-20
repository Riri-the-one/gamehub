package com.example.miniecommerce.services;

import com.example.miniecommerce.entities.Paniers;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public interface PaniersService {
    Paniers savePaniers(Paniers paniers);
    Paniers updatePaniers(Paniers paniers);
    void deletePaniers(Long id);
    Paniers getPaniers(Long id);
    void deleteAllPaniers();
    List<Paniers> getAllPaniers();
    Paniers getOrCreatePanierForUtilisateur(Long utilisateurId);
    Double calculateTotal(Long panierId);
    void clearPanier(Long panierId);
}
