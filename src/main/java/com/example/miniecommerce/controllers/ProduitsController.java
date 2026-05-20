package com.example.miniecommerce.controllers;

import com.example.miniecommerce.entities.Produits;
import com.example.miniecommerce.entities.Utilisateurs;
import com.example.miniecommerce.services.CategoriesService;
import com.example.miniecommerce.services.ProduitsService;
import com.example.miniecommerce.services.UtilisateurService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@AllArgsConstructor
public class ProduitsController {
    private ProduitsService produitsService;
    private CategoriesService categoriesService;
    private UtilisateurService utilisateurService;

    @RequestMapping("/")
    public String accueil(Model model, HttpSession session) {
        utilisateurService.checkAndGetCurrentUser(session);
        model.addAttribute("produits", produitsService.getAllProduits());
        model.addAttribute("categories", categoriesService.getAllCategories());
        return "accueil";
    }

    @RequestMapping("/produits")
    public String produits(@RequestParam(required = false) Long categoryId,
                           @RequestParam(required = false) String search,
                           Model model, HttpSession session) {
        utilisateurService.checkAndGetCurrentUser(session);
        model.addAttribute("produits", produitsService.getFilteredProducts(categoryId, search));
        model.addAttribute("categories", categoriesService.getAllCategories());
        model.addAttribute("searchKeyword", search);
        model.addAttribute("selectedCategoryId", categoryId);
        return "produits";
    }

    @RequestMapping("/createProduits")
    public String createProduits() {
        return "CreateProduits";
    }

    @RequestMapping("/saveProduits")
    public String saveProduits(@ModelAttribute("produits") Produits produits) {
        produitsService.saveProduits(produits);
        return "CreateProduits";
    }

    @RequestMapping("/produitsList")
    public String produitsList(ModelMap modelMap) {
        List<Produits> produits = produitsService.getAllProduits();
        modelMap.addAttribute("produits", produits);
        return "ProduitsList";
    }

    @RequestMapping("/deleteProduits")
    public String deleteProduits(@RequestParam("id") Long id, ModelMap modelMap) {
        produitsService.deleteProduits(id);
        List<Produits> produits = produitsService.getAllProduits();
        modelMap.addAttribute("produits", produits);
        return "ProduitsList";
    }

    @RequestMapping("/editProduits")
    public String editProduits(@RequestParam("id") Long id, ModelMap modelMap) {
        Produits produits = produitsService.getProduits(id);
        modelMap.addAttribute("produits", produits);
        return "EditProduits";
    }

    @RequestMapping("/updateProduits")
    public String updateProduits(@ModelAttribute("produits") Produits produits) {
        produitsService.updateProduits(produits);
        return "CreateProduits";
    }
}
