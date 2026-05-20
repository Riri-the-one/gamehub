package com.example.miniecommerce.services;

import com.example.miniecommerce.entities.Commandes;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public interface CommandesService {
    Commandes saveCommandes(Commandes commandes);
    Commandes updateCommandes(Commandes commandes);
    void deleteCommandes(Long id);
    Commandes getCommandes(Long id);
    void deleteAllCommandes();
    List<Commandes> getAllCommandes();
    Commandes passerCommande(Long utilisateurId, String adresseLivraison);
    void validerPaiementCommande(Long commandeId, String methode);
}
