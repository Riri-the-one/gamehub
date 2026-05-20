package com.example.miniecommerce.controllers;

import com.example.miniecommerce.entities.Commandes;
import com.example.miniecommerce.entities.Paiements;
import com.example.miniecommerce.entities.Paniers;
import com.example.miniecommerce.entities.Utilisateurs;
import com.example.miniecommerce.enums.CommandeEnum;
import com.example.miniecommerce.enums.PaiementEnum;
import com.example.miniecommerce.enums.StatutPaiementEnum;
import com.example.miniecommerce.services.CommandesService;
import com.example.miniecommerce.services.PaniersService;
import com.example.miniecommerce.services.UtilisateurService;
import com.example.miniecommerce.services.PaiementService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;
import java.util.List;

@Controller
@AllArgsConstructor
public class CommandesController {
    private CommandesService commandesService;
    private PaniersService paniersService;
    private UtilisateurService utilisateurService;
    private PaiementService paiementService;

    @RequestMapping("/checkout")
    public String checkout(Model model, HttpSession session) {
        Utilisateurs user = utilisateurService.checkAndGetCurrentUser(session);
        Paniers panier = paniersService.getOrCreatePanierForUtilisateur(user.getId());
        model.addAttribute("panier", panier);
        model.addAttribute("total", paniersService.calculateTotal(panier.getId()));
        return "checkout";
    }

    @PostMapping("/commander")
    public String commander(@RequestParam String adresse,
                            @RequestParam String prenom,
                            @RequestParam String nom,
                            @RequestParam String email,
                            HttpSession session) {
        Utilisateurs user = utilisateurService.checkAndGetCurrentUser(session);
        user.setPrenom(prenom);
        user.setNom(nom);
        user.setEmail(email);
        user = utilisateurService.saveUtilisateurs(user);
        session.setAttribute("currentUser", user);

        try {
            Commandes cmd = commandesService.passerCommande(user.getId(), adresse);
            return "redirect:/paiement/" + cmd.getId();
        } catch (Exception e) {
            return "redirect:/panier?erreur=" + e.getMessage();
        }
    }

    @RequestMapping("/paiement/{id}")
    public String paiement(@PathVariable Long id, Model model) {
        Commandes cmd = commandesService.getCommandes(id);
        model.addAttribute("commande", cmd);
        return "paiement";
    }

    @PostMapping("/commander/payer")
    public String validerPaiement(@RequestParam Long commandeId,
                                  @RequestParam String methode,
                                  HttpSession session) {
        commandesService.validerPaiementCommande(commandeId, methode);
        return "redirect:/confirmation/" + commandeId;
    }

    @RequestMapping("/confirmation/{id}")
    public String confirmation(@PathVariable Long id, Model model) {
        model.addAttribute("commande", commandesService.getCommandes(id));
        return "confirmation";
    }

    @RequestMapping("/createCommandes")
    public String createCommandes() {
        return "CreateCommandes";
    }

    @RequestMapping("/saveCommandes")
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

    @RequestMapping("/deleteCommandes")
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

    @RequestMapping("/updateCommandes")
    public String updateCommandes(@ModelAttribute("commandes") Commandes commandes) {
        commandesService.updateCommandes(commandes);
        return "CreateCommandes";
    }
}
