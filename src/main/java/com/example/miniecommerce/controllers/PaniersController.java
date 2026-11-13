package com.example.miniecommerce.controllers;

import com.example.miniecommerce.entities.Paniers;
import com.example.miniecommerce.services.GuestCartService;
import com.example.miniecommerce.services.PaniersService;
import jakarta.servlet.http.HttpServletRequest;
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
public class PaniersController {
    private PaniersService paniersService;


    private GuestCartService guestCartService;


    @GetMapping("/panier")
    public String voirPanier(Model model, HttpSession session) {
        Paniers panier = guestCartService.view(session);
        model.addAttribute("panier", panier);
        model.addAttribute("total", paniersService.calculateTotal(panier.getId()));
        return "panier";
    }

    @PostMapping("/panier/ajouter")
    public String ajouterAuPanier(@RequestParam Long produitId,
                                  @RequestParam(required = false) Integer quantite,
                                  HttpSession session, HttpServletRequest request) {
        Integer quantity = request.getParameter("quantite") == null ? Integer.valueOf(1) : quantite;
        guestCartService.add(session, produitId, quantity);
        return "redirect:/panier";
    }

    @PostMapping("/panier/supprimer/{id}")
    public String supprimerDuPanier(@PathVariable Long id, HttpSession session) {
        guestCartService.remove(session, id);
        return "redirect:/panier";
    }

    @PostMapping("/panier/vider")
    public String viderPanier(HttpSession session) {
        guestCartService.clear(session);
        return "redirect:/panier";
    }

    @RequestMapping("/createPaniers")
    public String createPaniers() {
        return "CreatePaniers";
    }

    @PostMapping("/savePaniers")
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

    @PostMapping("/deletePaniers")
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

    @PostMapping("/updatePaniers")
    public String updatePaniers(@ModelAttribute("paniers") Paniers paniers) {
        paniersService.updatePaniers(paniers);
        return "CreatePaniers";
    }
}
