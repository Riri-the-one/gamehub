package com.example.miniecommerce.controllers;

import com.example.miniecommerce.entities.Categories;
import com.example.miniecommerce.entities.Produits;
import com.example.miniecommerce.entities.Utilisateurs;
import com.example.miniecommerce.services.CategoriesService;
import com.example.miniecommerce.services.ProduitsService;
import com.example.miniecommerce.services.UtilisateurService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@AllArgsConstructor
public class UtilisateurController {
    
    private UtilisateurService utilisateurService;
    private ProduitsService produitsService;
    private CategoriesService categoriesService;


    @GetMapping("/admin/login")
    public String loginPage() {
        return "admin_login";
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("produits", produitsService.getAllProduits());
        model.addAttribute("categories", categoriesService.getAllCategories());
        return "admin_dashboard";
    }

    @GetMapping("/admin/add-category")
    public String addCategoryForm(Model model) {
        model.addAttribute("categorie", new Categories());
        return "admin_add_category";
    }

    @PostMapping("/admin/save-category")
    public String saveCategory(@ModelAttribute("categorie") Categories category) {
        categoriesService.saveCategories(category);
        return "redirect:/admin";
    }

    @GetMapping("/admin/add-product")
    public String addProductForm(Model model) {
        model.addAttribute("produit", new Produits());
        model.addAttribute("categories", categoriesService.getAllCategories());
        return "admin_add_product";
    }

    @PostMapping("/admin/save-product")
    public String saveProduct(@ModelAttribute("produit") Produits product) {
        produitsService.saveProduits(product);
        return "redirect:/admin";
    }

    @GetMapping("/admin/edit-product")
    public String editProductForm(@RequestParam("id") Long id, Model model) {
        model.addAttribute("produit", produitsService.getProduits(id));
        model.addAttribute("categories", categoriesService.getAllCategories());
        return "admin_edit_product";
    }

    @PostMapping("/admin/update-product")
    public String updateProduct(@ModelAttribute("produit") Produits product) {
        produitsService.updateProduits(product);
        return "redirect:/admin";
    }

    @PostMapping("/admin/delete-product")
    public String deleteProduct(@RequestParam("id") Long id) {
        produitsService.deleteProduits(id);
        return "redirect:/admin";
    }

    @PostMapping("/admin/delete-category")
    public String deleteCategory(@RequestParam("id") Long id) {
        categoriesService.deleteCategories(id);
        return "redirect:/admin";
    }

    @RequestMapping("/createUtilisateurs")
    public String createUtilisateurs() {
        return "CreateUtilisateurs";
    }

    @PostMapping("/saveUtilisateurs")
    public String saveUtilisateurs(@ModelAttribute("utilisateurs") Utilisateurs utilisateurs) {
        utilisateurService.saveUtilisateurs(utilisateurs);
        return "CreateUtilisateurs";
    }

    @RequestMapping("/utilisateursList")
    public String utilisateursList(ModelMap modelMap) {
        List<Utilisateurs> utilisateursList = utilisateurService.getAllUtilisateurs();
        modelMap.addAttribute("utilisateursList", utilisateursList);
        return "UtilisateursList";
    }

    @PostMapping("/deleteUtilisateurs")
    public String deleteUtilisateurs(@RequestParam("id") Long id, ModelMap modelMap) {
        utilisateurService.deleteUtilisateurs(id);
        List<Utilisateurs> utilisateursList = utilisateurService.getAllUtilisateurs();
        modelMap.addAttribute("utilisateursList", utilisateursList);
        return "UtilisateursList";
    }

    @RequestMapping("/editUtilisateurs")
    public String editUtilisateurs(@RequestParam("id") Long id, ModelMap modelMap) {
        Utilisateurs utilisateurs = utilisateurService.getUtilisateurs(id);
        modelMap.addAttribute("utilisateurs", utilisateurs);
        return "EditUtilisateurs";
    }

    @PostMapping("/updateUtilisateurs")
    public String updateUtilisateurs(@ModelAttribute("utilisateurs") Utilisateurs utilisateurs) {
        utilisateurService.updateUtilisateurs(utilisateurs);
        return "CreateUtilisateurs";
    }
}
