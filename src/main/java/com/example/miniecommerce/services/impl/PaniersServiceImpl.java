package com.example.miniecommerce.services.impl;

import com.example.miniecommerce.entities.LignePaniers;
import com.example.miniecommerce.entities.Paniers;
import com.example.miniecommerce.entities.Utilisateurs;
import com.example.miniecommerce.enums.UtilisateurRoleEnum;
import com.example.miniecommerce.repositories.LignePaniersRepositorie;
import com.example.miniecommerce.repositories.PaniersRepositorie;
import com.example.miniecommerce.repositories.ProduitsRepositorie;
import com.example.miniecommerce.repositories.UtilisateursRepositorie;
import com.example.miniecommerce.services.Money;
import com.example.miniecommerce.services.PaniersService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class PaniersServiceImpl implements PaniersService {

    private PaniersRepositorie paniersRepositorie;
    private UtilisateursRepositorie utilisateursRepositorie;
    private LignePaniersRepositorie lignePaniersRepositorie;
    private ProduitsRepositorie produitsRepositorie;
    public static final int MAX_QUANTITY = 99;

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
    @Transactional
    public Paniers getOrCreatePanierForUtilisateur(Long utilisateurId) {
        Paniers panier = lockedCart(utilisateurId);
        // Materialize the owner-scoped lines while the cart is locked.
        panier.getLignePaniers().size();
        return panier;
    }

    private Paniers lockedCart(Long utilisateurId) {
        Utilisateurs user = utilisateursRepositorie.findLockedById(utilisateurId)
                .filter(u -> u.getUtilisateurRole() == UtilisateurRoleEnum.Client)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        // Lock the existing owner row even before the first cart exists. All guest
        // writers take this same lock, so creation and line merges are serialized.
        return paniersRepositorie.findByUtilisateurs_Id(utilisateurId).orElseGet(() -> {
            Paniers cart = new Paniers();
            cart.setDate(new Date());
            cart.setUtilisateurs(user);
            cart.setLignePaniers(new ArrayList<>());
            return paniersRepositorie.save(cart);
        });
    }

    @Override
    @Transactional
    public void addForUtilisateur(Long utilisateurId, Long produitId, Integer quantite) {
        if (quantite == null || quantite < 1 || quantite > MAX_QUANTITY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantité autorisée : 1 à 99.");
        }
        Paniers cart = lockedCart(utilisateurId);
        var product = produitsRepositorie.findById(produitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var existing = lignePaniersRepositorie.findByPaniers_IdAndProduits_Id(cart.getId(), produitId);
        long quantity = (long) quantite + existing.map(LignePaniers::getQuantite).orElse(0);
        if (quantity > MAX_QUANTITY || product.getQuantite() == null || quantity > product.getQuantite()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantité indisponible ou limite de 99 dépassée.");
        }
        LignePaniers line = existing.orElseGet(LignePaniers::new);
        line.setPaniers(cart);
        line.setProduits(product);
        line.setQuantite((int) quantity);
        lignePaniersRepositorie.save(line);
    }

    @Override
    @Transactional
    public void removeForUtilisateur(Long utilisateurId, Long ligneId) {
        Paniers cart = lockedCart(utilisateurId);
        LignePaniers line = lignePaniersRepositorie.findByIdAndPaniers_Id(ligneId, cart.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        lignePaniersRepositorie.delete(line);
    }

    @Override
    @Transactional
    public void clearForUtilisateur(Long utilisateurId) {
        Paniers cart = lockedCart(utilisateurId);
        lignePaniersRepositorie.deleteAll(lignePaniersRepositorie.findByPaniers_Id(cart.getId()));
    }

    @Override
    public BigDecimal calculateTotal(Long panierId) {
        Paniers panier = paniersRepositorie.findById(panierId).orElse(null);
        if (panier == null || panier.getLignePaniers() == null) {
            return Money.ZERO;
        }

        BigDecimal total = Money.ZERO;
        for (LignePaniers lp : panier.getLignePaniers()) {
            if (lp.getProduits() != null && lp.getProduits().getPrix() != null) {
                total = total.add(lp.getProduits().getPrix().multiply(BigDecimal.valueOf(lp.getQuantite())));
            }
        }
        return Money.amount(total);
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
