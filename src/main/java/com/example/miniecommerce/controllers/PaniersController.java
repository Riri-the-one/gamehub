package com.example.miniecommerce.controllers;

import com.example.miniecommerce.entities.Paniers;
import com.example.miniecommerce.entities.Utilisateurs;
import com.example.miniecommerce.services.LignePaniersService;
import com.example.miniecommerce.services.PaniersService;
import com.example.miniecommerce.services.UtilisateurService;
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

import java.util.List;

@Controller
@AllArgsConstructor
public class PaniersController {
    private PaniersService paniersService;
    private LignePaniersService lignePaniersService;
    private UtilisateurService utilisateurService;


    @RequestMapping("/panier")
    public String voirPanier(Model model, HttpSession session) {
        Utilisateurs user = utilisateurService.checkAndGetCurrentUser(session);
        Paniers panier = paniersService.getOrCreatePanierForUtilisateur(user.getId());
        model.addAttribute("panier", panier);
        model.addAttribute("total", paniersService.calculateTotal(panier.getId()));
        return "panier";
    }

    @PostMapping("/panier/ajouter")
    public String ajouterAuPanier(@RequestParam Long produitId,
                                  @RequestParam(defaultValue = "1") Integer quantite,
                                  HttpSession session) {
        Utilisateurs user = utilisateurService.checkAndGetCurrentUser(session);
        Paniers panier = paniersService.getOrCreatePanierForUtilisateur(user.getId());
        lignePaniersService.addProduitToPanier(panier.getId(), produitId, quantite);
        return "redirect:/panier";
    }

    @RequestMapping("/panier/supprimer/{id}")
    public String supprimerDuPanier(@PathVariable Long id) {
        lignePaniersService.deleteLignePaniers(id);
        return "redirect:/panier";
    }

    @RequestMapping("/panier/vider")
    public String viderPanier(HttpSession session) {
        Utilisateurs user = utilisateurService.checkAndGetCurrentUser(session);
        Paniers panier = paniersService.getOrCreatePanierForUtilisateur(user.getId());
        paniersService.clearPanier(panier.getId());
        return "redirect:/panier";
    }

    @RequestMapping("/createPaniers")
    public String createPaniers() {
        return "CreatePaniers";
    }

    @RequestMapping("/savePaniers")
    public String savePaniers(@ModelAttribute("paniers") Paniers paniers) {
        paniersService.savePaniers(paniers);
        return "CreatePaniers";
    }

    @RequestMapping("/paniersList")
    public String paniersList(ModelMap modelMap) {
        List<Paniers> paniers = paniersService.getAllPaniers();
        modelMap.addAttribute("paniers", paniers);
        return "PaniersList";
    }

    @RequestMapping("/deletePaniers")
    public String deletePaniers(@RequestParam("id") Long id, ModelMap modelMap) {
        paniersService.deletePaniers(id);
        List<Paniers> paniers = paniersService.getAllPaniers();
        modelMap.addAttribute("paniers", paniers);
        return "PaniersList";
    }

    @RequestMapping("/editPaniers")
    public String editPaniers(@RequestParam("id") Long id, ModelMap modelMap) {
        Paniers paniers = paniersService.getPaniers(id);
        modelMap.addAttribute("paniers", paniers);
        return "EditPaniers";
    }

    @RequestMapping("/updatePaniers")
    public String updatePaniers(@ModelAttribute("paniers") Paniers paniers) {
        paniersService.updatePaniers(paniers);
        return "CreatePaniers";
    }
}
