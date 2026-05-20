package com.example.miniecommerce.controllers;

import com.example.miniecommerce.entities.Categories;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

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

    @PostMapping("/admin/login")
    public String login(@RequestParam String email, @RequestParam String password, HttpSession session, Model model) {
        return utilisateurService.handleAdminLogin(email, password, session, model);
    }

    @GetMapping("/admin/logout")
    public String logout(HttpSession session) {
        utilisateurService.logoutAdmin(session);
        return "redirect:/admin/login";
    }

    @GetMapping("/admin")
    public String dashboard(HttpSession session, Model model) {
        if (!utilisateurService.isAdmin(session)) {
            return "redirect:/admin/login";
        }
        model.addAttribute("produits", produitsService.getAllProduits());
        model.addAttribute("categories", categoriesService.getAllCategories());
        return "admin_dashboard";
    }

    @GetMapping("/admin/add-category")
    public String addCategoryForm(HttpSession session, Model model) {
        if (!utilisateurService.isAdmin(session)) {
            return "redirect:/admin/login";
        }
        model.addAttribute("categorie", new Categories());
        return "admin_add_category";
    }

    @PostMapping("/admin/save-category")
    public String saveCategory(@ModelAttribute("categorie") Categories category, HttpSession session) {
        if (!utilisateurService.isAdmin(session)) {
            return "redirect:/admin/login";
        }
        categoriesService.saveCategories(category);
        return "redirect:/admin";
    }

    @GetMapping("/admin/add-product")
    public String addProductForm(HttpSession session, Model model) {
        if (!utilisateurService.isAdmin(session)) {
            return "redirect:/admin/login";
        }
        model.addAttribute("produit", new Produits());
        model.addAttribute("categories", categoriesService.getAllCategories());
        return "admin_add_product";
    }

    @PostMapping("/admin/save-product")
    public String saveProduct(@ModelAttribute("produit") Produits product, HttpSession session) {
        if (!utilisateurService.isAdmin(session)) {
            return "redirect:/admin/login";
        }
        produitsService.saveProduits(product);
        return "redirect:/admin";
    }

    @GetMapping("/admin/edit-product")
    public String editProductForm(@RequestParam("id") Long id, HttpSession session, Model model) {
        if (!utilisateurService.isAdmin(session)) {
            return "redirect:/admin/login";
        }
        model.addAttribute("produit", produitsService.getProduits(id));
        model.addAttribute("categories", categoriesService.getAllCategories());
        return "admin_edit_product";
    }

    @PostMapping("/admin/update-product")
    public String updateProduct(@ModelAttribute("produit") Produits product, HttpSession session) {
        if (!utilisateurService.isAdmin(session)) {
            return "redirect:/admin/login";
        }
        produitsService.updateProduits(product);
        return "redirect:/admin";
    }

    @GetMapping("/admin/delete-product")
    public String deleteProduct(@RequestParam("id") Long id, HttpSession session) {
        if (!utilisateurService.isAdmin(session)) {
            return "redirect:/admin/login";
        }
        produitsService.deleteProduits(id);
        return "redirect:/admin";
    }

    @GetMapping("/admin/delete-category")
    public String deleteCategory(@RequestParam("id") Long id, HttpSession session) {
        if (!utilisateurService.isAdmin(session)) {
            return "redirect:/admin/login";
        }
        categoriesService.deleteCategories(id);
        return "redirect:/admin";
    }

    @RequestMapping("/createUtilisateurs")
    public String createUtilisateurs() {
        return "CreateUtilisateurs";
    }

    @RequestMapping("/saveUtilisateurs")
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

    @RequestMapping("/deleteUtilisateurs")
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

    @RequestMapping("/updateUtilisateurs")
    public String updateUtilisateurs(@ModelAttribute("utilisateurs") Utilisateurs utilisateurs) {
        utilisateurService.updateUtilisateurs(utilisateurs);
        return "CreateUtilisateurs";
    }
}
