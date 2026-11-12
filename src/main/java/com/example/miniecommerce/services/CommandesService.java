package com.example.miniecommerce.services;

import com.example.miniecommerce.entities.Commandes;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public interface CommandesService {
    Commandes saveCommandes(Commandes commandes);
    Commandes updateCommandes(Commandes commandes);
    void deleteCommandes(Long id);
    Commandes getCommandes(Long id);
    void deleteAllCommandes();
    List<Commandes> getAllCommandes();
    Commandes passerCommande(Long utilisateurId, CheckoutDetails details);
    Commandes passerCommande(Long utilisateurId, CheckoutDetails details, String token);
    Optional<Commandes> replay(Long utilisateurId, String token, CheckoutDetails details);
    void cancel(Long utilisateurId, Long commandeId);
    void expire(Long utilisateurId, Long commandeId, Date cutoff);
    Commandes getOwnedCommande(Long utilisateurId, Long commandeId);
    void validerPaiementCommande(Long utilisateurId, Long commandeId, String methode);
}
