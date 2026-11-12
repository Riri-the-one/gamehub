package com.example.miniecommerce.controllers;

import com.example.miniecommerce.entities.Commandes;
import com.example.miniecommerce.entities.Paniers;
import com.example.miniecommerce.entities.Utilisateurs;
import com.example.miniecommerce.enums.CommandeEnum;
import com.example.miniecommerce.enums.StatutPaiementEnum;
import com.example.miniecommerce.services.CheckoutDetails;
import com.example.miniecommerce.services.CommandesService;
import com.example.miniecommerce.services.GuestOrderService;
import com.example.miniecommerce.services.PaniersService;
import com.example.miniecommerce.services.UtilisateurService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@AllArgsConstructor
public class CommandesController {
    private CommandesService commandesService;
    private PaniersService paniersService;
    private UtilisateurService utilisateurService;
    private GuestOrderService guestOrderService;

    @GetMapping("/checkout")
    public String checkout(Model model, HttpSession session) {
        Utilisateurs user = utilisateurService.checkAndGetCurrentUser(session);
        Paniers panier = paniersService.getOrCreatePanierForUtilisateur(user.getId());
        model.addAttribute("panier", panier);
        model.addAttribute("total", paniersService.calculateTotal(panier.getId()));
        model.addAttribute("checkoutToken", guestOrderService.checkoutToken(session));
        return "checkout";
    }

    @PostMapping("/commander")
    public String commander(@RequestParam String adresse, @RequestParam String prenom,
                            @RequestParam String nom, @RequestParam String email,
                            @RequestParam(required = false) String checkoutToken, HttpSession session) {
        Commandes order = guestOrderService.submit(session, checkoutToken,
                new CheckoutDetails(prenom, nom, email, adresse));
        return "redirect:/paiement/" + order.getId();
    }

    @GetMapping("/paiement/{id}")
    public String paiement(@PathVariable Long id, Model model, HttpSession session) {
        Commandes order = guestOrderService.view(session, id);
        if (order.getCommandestatut() != CommandeEnum.EN_ATTENTE) return "redirect:/confirmation/" + id;
        model.addAttribute("commande", order);
        return "paiement";
    }

    @PostMapping("/commander/payer")
    public String validerPaiement(@RequestParam Long commandeId, @RequestParam String methode, HttpSession session) {
        guestOrderService.pay(session, commandeId, methode);
        return "redirect:/confirmation/" + commandeId;
    }

    @PostMapping("/commander/annuler")
    public String annuler(@RequestParam Long commandeId,HttpSession session) {
        guestOrderService.cancel(session,commandeId);
        return "redirect:/confirmation/"+commandeId;
    }

    @GetMapping("/confirmation/{id}")
    public String confirmation(@PathVariable Long id, Model model, HttpSession session) {
        Commandes order = guestOrderService.view(session, id);
        boolean paid = order.getPaiements() != null && order.getPaiements().getStatutPaiement() == StatutPaiementEnum.APPROUVE
                && (order.getCommandestatut() == CommandeEnum.CONFIRMEE || order.getCommandestatut() == CommandeEnum.EN_LIVRAISON
                || order.getCommandestatut() == CommandeEnum.LIVREE);
        model.addAttribute("commande", order);
        model.addAttribute("paid", paid);
        model.addAttribute("confirmationTitle", paid ? "Paiement simulé validé" : "État de la commande");
        model.addAttribute("confirmationMessage", paid ? "Simulation pédagogique réussie. Aucun débit réel n’a été effectué."
                : "Cette page affiche l’état actuel de la commande ; elle ne prouve pas un paiement.");
        String state = order.getCommandestatut() == null ? "État inconnu" : switch (order.getCommandestatut()) {
            case EN_ATTENTE -> "En attente de paiement";
            case CONFIRMEE -> "Confirmée";
            case EN_LIVRAISON -> "En livraison";
            case LIVREE -> "Livrée";
            case ANNULEE -> "Annulée";
        };
        model.addAttribute("confirmationState", state);
        return "confirmation";
    }
    @RequestMapping("/createCommandes")
    public String createCommandes() {
        return "CreateCommandes";
    }

    @PostMapping("/saveCommandes")
    public String saveCommandes(@ModelAttribute("commandes") Commandes commandes) {
        commandesService.saveCommandes(commandes);
        return "CreateCommandes";
    }

    @RequestMapping("/commandesList")
    public String commandesList(ModelMap modelMap) {
        List<Commandes> commandes = commandesService.getAllCommandes();
        modelMap.addAttribute("commandes", commandes);
        return "CommandesList";
    }

    @PostMapping("/deleteCommandes")
    public String deleteCommandes(@RequestParam("id") Long id, ModelMap modelMap) {
        commandesService.deleteCommandes(id);
        List<Commandes> commandes = commandesService.getAllCommandes();
        modelMap.addAttribute("commandes", commandes);
        return "CommandesList";
    }

    @RequestMapping("/editCommandes")
    public String editCommandes(@RequestParam("id") Long id, ModelMap modelMap) {
        Commandes commandes = commandesService.getCommandes(id);
        modelMap.addAttribute("commandes", commandes);
        return "EditCommandes";
    }

    @PostMapping("/updateCommandes")
    public String updateCommandes(@ModelAttribute("commandes") Commandes commandes) {
        commandesService.updateCommandes(commandes);
        return "CreateCommandes";
    }
}
