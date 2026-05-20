package com.example.miniecommerce.services.impl;

import com.example.miniecommerce.entities.Commandes;
import com.example.miniecommerce.entities.LigneCommandes;
import com.example.miniecommerce.entities.LignePaniers;
import com.example.miniecommerce.entities.Paniers;
import com.example.miniecommerce.entities.Produits;
import com.example.miniecommerce.entities.Utilisateurs;
import com.example.miniecommerce.enums.CommandeEnum;
import com.example.miniecommerce.entities.Paiements;
import com.example.miniecommerce.enums.PaiementEnum;
import com.example.miniecommerce.enums.StatutPaiementEnum;
import com.example.miniecommerce.repositories.PaiementsRepositorie;
import com.example.miniecommerce.repositories.CommandesRepositorie;
import com.example.miniecommerce.repositories.LigneCommandesRepositorie;
import com.example.miniecommerce.repositories.LignePaniersRepositorie;
import com.example.miniecommerce.repositories.PaniersRepositorie;
import com.example.miniecommerce.repositories.ProduitsRepositorie;
import com.example.miniecommerce.repositories.UtilisateursRepositorie;
import com.example.miniecommerce.services.CommandesService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@AllArgsConstructor
public class CommandesServiceImpl implements CommandesService {

    private CommandesRepositorie commandesRepositorie;
    private UtilisateursRepositorie utilisateursRepositorie;
    private PaniersRepositorie paniersRepositorie;
    private LignePaniersRepositorie lignePaniersRepositorie;
    private ProduitsRepositorie produitsRepositorie;
    private LigneCommandesRepositorie ligneCommandesRepositorie;
    private PaiementsRepositorie paiementsRepositorie;

    @Override
    public Commandes saveCommandes(Commandes commandes) {
        return commandesRepositorie.save(commandes);
    }

    @Override
    public Commandes updateCommandes(Commandes commandes) {
        return commandesRepositorie.save(commandes);
    }

    @Override
    public void deleteCommandes(Long id) {
        commandesRepositorie.deleteById(id);
    }

    @Override
    public Commandes getCommandes(Long id) {
        return commandesRepositorie.findById(id).orElse(null);
    }

    @Override
    public void deleteAllCommandes() {
        commandesRepositorie.deleteAll();
    }

    @Override
    public List<Commandes> getAllCommandes() {
        return commandesRepositorie.findAll();
    }

    @Override
    public Commandes passerCommande(Long utilisateurId, String adresseLivraison) {
        Utilisateurs user = utilisateursRepositorie.findById(utilisateurId).orElse(null);
        if (user == null) {
            throw new RuntimeException("Utilisateur non trouvé !");
        }

        Paniers panier = null;
        for (Paniers p : paniersRepositorie.findAll()) {
            if (p.getUtilisateurs() != null && p.getUtilisateurs().getId().equals(utilisateurId)) {
                panier = p;
                break;
            }
        }
        if (panier == null) {
            throw new RuntimeException("Panier non trouvé !");
        }

        List<LignePaniers> cartLines = panier.getLignePaniers();
        if (cartLines == null || cartLines.isEmpty()) {
            throw new RuntimeException("Le panier est vide. Impossible de commander !");
        }
        double total = 0.0;
        for (LignePaniers lp : cartLines) {
            if (lp.getProduits() != null && lp.getProduits().getPrix() != null) {
                total += lp.getProduits().getPrix() * lp.getQuantite();
            }
        }

        Commandes commande = new Commandes();
        commande.setDateCommande(new Date());
        commande.setCommandestatut(CommandeEnum.EN_ATTENTE);
        commande.setMontant(total);
        commande.setAdresseDeLivraison(adresseLivraison);
        commande.setUtilisateur(user);
        commande.setLigneCommandes(new ArrayList<>());

        Commandes savedCommande = commandesRepositorie.save(commande);
        List<LigneCommandes> orderLines = new ArrayList<>();
        for (LignePaniers lp : cartLines) {
            Produits prod = lp.getProduits();
            if (prod != null) {
                int currentQty = prod.getQuantite() != null ? prod.getQuantite() : 0;
                int orderedQty = lp.getQuantite() != null ? lp.getQuantite() : 0;

                if (currentQty < orderedQty) {
                    throw new RuntimeException("Stock insuffisant pour le produit : " + prod.getNom());
                }

                prod.setQuantite(currentQty - orderedQty);
                produitsRepositorie.save(prod);
                LigneCommandes lc = new LigneCommandes();
                lc.setQuantite(orderedQty);
                lc.setPrixUnitaire(prod.getPrix() != null ? prod.getPrix() : 0.0);
                lc.setProduit(prod);
                lc.setCommandes(savedCommande);

                LigneCommandes savedLc = ligneCommandesRepositorie.save(lc);
                orderLines.add(savedLc);
            }
        }

        savedCommande.setLigneCommandes(orderLines);

        lignePaniersRepositorie.deleteAll(cartLines);
        cartLines.clear();
        paniersRepositorie.save(panier);

        return savedCommande;
    }

    @Override
    public void validerPaiementCommande(Long commandeId, String methode) {
        Commandes cmd = getCommandes(commandeId);
        if (cmd != null) {
            cmd.setCommandestatut(CommandeEnum.CONFIRMEE);
            updateCommandes(cmd);

            Paiements p = new Paiements();
            p.setCommandes(cmd);
            p.setMontant(cmd.getMontant());
            p.setDatePaiement(new Date());
            p.setStatutPaiement(StatutPaiementEnum.APPROUVE);
            p.setMethodePaiement(PaiementEnum.valueOf(methode));
            paiementsRepositorie.save(p);
        }
    }
}
