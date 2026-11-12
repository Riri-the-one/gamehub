package com.example.miniecommerce.services.impl;

import com.example.miniecommerce.entities.Commandes;
import com.example.miniecommerce.entities.LigneCommandes;
import com.example.miniecommerce.entities.LignePaniers;
import com.example.miniecommerce.entities.Paiements;
import com.example.miniecommerce.entities.Paniers;
import com.example.miniecommerce.entities.Produits;
import com.example.miniecommerce.entities.Utilisateurs;
import com.example.miniecommerce.enums.CommandeEnum;
import com.example.miniecommerce.enums.PaiementEnum;
import com.example.miniecommerce.enums.StatutPaiementEnum;
import com.example.miniecommerce.enums.UtilisateurRoleEnum;
import com.example.miniecommerce.repositories.CommandesRepositorie;
import com.example.miniecommerce.repositories.LigneCommandesRepositorie;
import com.example.miniecommerce.repositories.LignePaniersRepositorie;
import com.example.miniecommerce.repositories.PaiementsRepositorie;
import com.example.miniecommerce.repositories.PaniersRepositorie;
import com.example.miniecommerce.repositories.ProduitsRepositorie;
import com.example.miniecommerce.repositories.UtilisateursRepositorie;
import com.example.miniecommerce.services.CheckoutDetails;
import com.example.miniecommerce.services.CommandesService;
import com.example.miniecommerce.services.Money;
import com.example.miniecommerce.services.Secrets;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    private EntityManager entityManager;

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

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private Produits lockProduct(Long id) {
        try {
            Produits product=produitsRepositorie.findForUpdate(id).orElseThrow(() -> conflict("Produit indisponible."));
            entityManager.refresh(product,LockModeType.PESSIMISTIC_WRITE);
            return product;
        } catch (org.springframework.dao.OptimisticLockingFailureException | org.springframework.dao.PessimisticLockingFailureException changed) {
            throw conflict("Le stock ou le produit a changé. Rechargez la page avant de réessayer.");
        }
    }

    private void validText(String value) {
        if (value == null || value.isBlank() || value.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informations de livraison invalides.");
        }
    }

    @Override
    @Transactional
    public Commandes passerCommande(Long utilisateurId, CheckoutDetails details) {
        return passerCommande(utilisateurId, details, Secrets.token());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Commandes> replay(Long utilisateurId, String token, CheckoutDetails details) {
        var previous=commandesRepositorie.findByCheckoutKey(Secrets.hash(token));
        if (previous.isPresent()) {
            if (!utilisateurId.equals(previous.get().getUtilisateur().getId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            if (!Secrets.fingerprint(details).equals(previous.get().getCheckoutFingerprint())) throw conflict("Cette demande a déjà été utilisée avec d’autres informations.");
        }
        return previous;
    }

    @Override
    @Transactional
    public Commandes passerCommande(Long utilisateurId, CheckoutDetails details, String token) {
        validText(details.prenom()); validText(details.nom()); validText(details.email()); validText(details.adresse());
        if (!details.email().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Adresse email invalide.");
        }
        Utilisateurs user = utilisateursRepositorie.findLockedById(utilisateurId)
                .filter(u -> u.getUtilisateurRole() == UtilisateurRoleEnum.Client)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var previous=replay(utilisateurId,token,details);
        if (previous.isPresent()) return previous.get();
        Paniers cart = paniersRepositorie.findByUtilisateurs_Id(utilisateurId)
                .orElseThrow(() -> conflict("Le panier est vide."));
        List<LignePaniers> cartLines = lignePaniersRepositorie.findByPaniers_Id(cart.getId());
        if (cartLines.isEmpty()) throw conflict("Le panier est vide.");

        // Use the same owner lock as guest cart writers, then product locks in ID order.
        var quantities = new TreeMap<Long, Integer>();
        for (LignePaniers line : cartLines) {
            if (line.getProduits() == null || line.getQuantite() == null || line.getQuantite() < 1 || line.getQuantite() > 99) {
                throw conflict("Le panier contient une ligne invalide.");
            }
            long quantity = (long) quantities.getOrDefault(line.getProduits().getId(), 0) + line.getQuantite();
            if (quantity > 99) throw conflict("La quantité cumulée dépasse 99.");
            quantities.put(line.getProduits().getId(), (int) quantity);
        }
        var lockedProducts = new LinkedHashMap<Long, Produits>();
        BigDecimal total = Money.ZERO;
        for (var entry : quantities.entrySet()) {
            Produits product = lockProduct(entry.getKey());
            // Refresh even if Open EntityManager in View loaded this product earlier.
            entityManager.refresh(product, LockModeType.PESSIMISTIC_WRITE);
            if (product.getPrix() == null || product.getPrix().signum() < 0) {
                throw conflict("Prix de produit invalide.");
            }
            if (product.getQuantite() == null || product.getQuantite() < entry.getValue()) {
                throw conflict("Stock insuffisant.");
            }
            total = Money.amount(total.add(Money.price(product.getPrix()).multiply(BigDecimal.valueOf(entry.getValue()))));

            lockedProducts.put(entry.getKey(), product);
        }

        user.setPrenom(details.prenom()); user.setNom(details.nom()); user.setEmail(details.email());
        Commandes order = new Commandes();
        order.setCheckoutKey(Secrets.hash(token)); order.setCheckoutFingerprint(Secrets.fingerprint(details));
        order.setUtilisateur(user); order.setAdresseDeLivraison(details.adresse());
        order.setDateCommande(new Date()); order.setCommandestatut(CommandeEnum.EN_ATTENTE); order.setMontant(total);
        order = commandesRepositorie.save(order);
        var orderLines = new ArrayList<LigneCommandes>();
        for (var entry : quantities.entrySet()) {
            Produits product = lockedProducts.get(entry.getKey());
            if (produitsRepositorie.decrementAvailableStock(product.getId(), entry.getValue()) != 1) {
                throw conflict("Stock insuffisant.");
            }
            entityManager.refresh(product, LockModeType.PESSIMISTIC_WRITE);
            LigneCommandes line = new LigneCommandes();
            line.setCommandes(order); line.setProduit(product); line.setQuantite(entry.getValue());
            line.setPrixUnitaire(product.getPrix());
            orderLines.add(ligneCommandesRepositorie.save(line));
        }
        order.setLigneCommandes(orderLines);
        lignePaniersRepositorie.deleteAll(cartLines);
        cart.getLignePaniers().clear();
        return order;
    }

    @Override
    @Transactional(readOnly = true)
    public Commandes getOwnedCommande(Long utilisateurId, Long commandeId) {
        return commandesRepositorie.findByIdAndUtilisateur_Id(commandeId, utilisateurId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Override
    @Transactional
    public void validerPaiementCommande(Long utilisateurId, Long commandeId, String methode) {
        utilisateursRepositorie.findLockedById(utilisateurId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Commandes order = commandesRepositorie.findOwnedForUpdate(commandeId, utilisateurId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        entityManager.refresh(order, LockModeType.PESSIMISTIC_WRITE);
        PaiementEnum method;
        try { method = PaiementEnum.valueOf(methode); }
        catch (IllegalArgumentException | NullPointerException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Méthode de paiement invalide.");
        }
        if (order.getMontant() == null || order.getMontant().signum() < 0) {
            throw conflict("Montant de commande invalide.");
        }
        var existing = paiementsRepositorie.findByCommandes_Id(commandeId);
        if (existing.isPresent()) {
            Paiements payment = existing.get();
            boolean confirmed = order.getCommandestatut() == CommandeEnum.CONFIRMEE
                    || order.getCommandestatut() == CommandeEnum.EN_LIVRAISON || order.getCommandestatut() == CommandeEnum.LIVREE;
            if (confirmed && payment.getStatutPaiement() == StatutPaiementEnum.APPROUVE
                    && payment.getMethodePaiement() == method && Objects.equals(payment.getMontant(), order.getMontant())) return;
            throw conflict("Le paiement existant ne permet pas cette opération.");
        }
        if (order.getCommandestatut() != CommandeEnum.EN_ATTENTE) throw conflict("Cette commande ne peut pas être payée.");
        if(commandesRepositorie.transition(commandeId,utilisateurId,CommandeEnum.EN_ATTENTE,CommandeEnum.CONFIRMEE)!=1) {
            throw conflict("L’état de la commande a changé.");
        }
        entityManager.refresh(order,LockModeType.PESSIMISTIC_WRITE);
        Paiements payment = new Paiements();
        payment.setCommandes(order); payment.setMontant(order.getMontant()); payment.setDatePaiement(new Date());
        payment.setMethodePaiement(method); payment.setStatutPaiement(StatutPaiementEnum.APPROUVE);
        paiementsRepositorie.save(payment);
        order.setPaiements(payment);
    }

    @Override
    @Transactional
    public void cancel(Long utilisateurId, Long commandeId) { cancelPending(utilisateurId, commandeId, null); }

    @Override
    @Transactional
    public void expire(Long utilisateurId, Long commandeId, Date cutoff) { cancelPending(utilisateurId, commandeId, cutoff); }

    private void cancelPending(Long owner, Long id, Date cutoff) {
        utilisateursRepositorie.findLockedById(owner).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Commandes order=commandesRepositorie.findOwnedForUpdate(id,owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        entityManager.refresh(order,LockModeType.PESSIMISTIC_WRITE);
        if (order.getCommandestatut()==CommandeEnum.ANNULEE) return;
        if (cutoff!=null && (order.getCheckoutKey()==null || order.getDateCommande()==null
                || !order.getDateCommande().before(cutoff) || order.getCommandestatut()!=CommandeEnum.EN_ATTENTE)) return;
        if (order.getCommandestatut()!=CommandeEnum.EN_ATTENTE || paiementsRepositorie.findByCommandes_Id(id).isPresent()) {
            throw conflict("Seule une commande en attente sans paiement peut être annulée.");
        }
        if(commandesRepositorie.transition(id,owner,CommandeEnum.EN_ATTENTE,CommandeEnum.ANNULEE)!=1) {
            throw conflict("L’état de la commande a changé.");
        }
        entityManager.refresh(order,LockModeType.PESSIMISTIC_WRITE);
        var quantities=new TreeMap<Long,Integer>();
        for(var line:order.getLigneCommandes()) {
            if(line.getProduit()==null || line.getQuantite()==null || line.getQuantite()<1) throw conflict("Ligne historique invalide.");
            quantities.merge(line.getProduit().getId(),line.getQuantite(),Math::addExact);
        }
        for(var entry:quantities.entrySet()) {
            var product=lockProduct(entry.getKey());
            entityManager.refresh(product,LockModeType.PESSIMISTIC_WRITE);
            if(product.getQuantite()==null || (long)product.getQuantite()+entry.getValue()>Integer.MAX_VALUE) throw conflict("Stock invalide.");
            product.setQuantite(product.getQuantite()+entry.getValue());
        }
    }
}
