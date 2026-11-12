package com.example.miniecommerce.controllers;

import com.example.miniecommerce.entities.LigneCommandes;
import com.example.miniecommerce.services.LigneCommandesService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@AllArgsConstructor
public class LigneCommandesController {
    private LigneCommandesService ligneCommandesService;

    @RequestMapping("/createLigneCommandes")
    public String createLigneCommandes() {
        return "CreateLigneCommandes";
    }

    @PostMapping("/saveLigneCommandes")
    public String saveLigneCommandes(@ModelAttribute("ligneCommandes") LigneCommandes ligneCommandes) {
        ligneCommandesService.saveLigneCommandes(ligneCommandes);
        return "CreateLigneCommandes";
    }

    @RequestMapping("/ligneCommandesList")
    public String ligneCommandesList(ModelMap modelMap) {
        List<LigneCommandes> ligneCommandes = ligneCommandesService.getAllLigneCommandes();
        modelMap.addAttribute("ligneCommandes", ligneCommandes);
        return "LigneCommandesList";
    }

    @PostMapping("/deleteLigneCommandes")
    public String deleteLigneCommandes(@RequestParam("id") Long id, ModelMap modelMap) {
        ligneCommandesService.deleteLigneCommandes(id);
        List<LigneCommandes> ligneCommandes = ligneCommandesService.getAllLigneCommandes();
        modelMap.addAttribute("ligneCommandes", ligneCommandes);
        return "LigneCommandesList";
    }

    @RequestMapping("/editLigneCommandes")
    public String editLigneCommandes(@RequestParam("id") Long id, ModelMap modelMap) {
        LigneCommandes ligneCommandes = ligneCommandesService.getLigneCommandes(id);
        modelMap.addAttribute("ligneCommandes", ligneCommandes);
        return "EditLigneCommandes";
    }

    @PostMapping("/updateLigneCommandes")
    public String updateLigneCommandes(@ModelAttribute("ligneCommandes") LigneCommandes ligneCommandes) {
        ligneCommandesService.updateLigneCommandes(ligneCommandes);
        return "CreateLigneCommandes";
    }
}
