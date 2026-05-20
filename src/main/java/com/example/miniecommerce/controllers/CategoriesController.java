package com.example.miniecommerce.controllers;

import com.example.miniecommerce.entities.Categories;
import com.example.miniecommerce.services.CategoriesService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@AllArgsConstructor
public class CategoriesController {
    private CategoriesService categoriesService;

    @RequestMapping("/createCategories")
    public String createCategories() {
        return "CreateCategories";
    }

    @RequestMapping("/saveCategories")
    public String saveCategories(@ModelAttribute("categories") Categories categories) {
        categoriesService.saveCategories(categories);
        return "CreateCategories";
    }

    @RequestMapping("/categoriesList")
    public String categoriesList(ModelMap modelMap) {
        List<Categories> categories = categoriesService.getAllCategories();
        modelMap.addAttribute("categories", categories);
        return "CategoriesList";
    }

    @RequestMapping("/deleteCategories")
    public String deleteCategories(@RequestParam("id") Long id, ModelMap modelMap) {
        categoriesService.deleteCategories(id);
        List<Categories> categories = categoriesService.getAllCategories();
        modelMap.addAttribute("categories", categories);
        return "CategoriesList";
    }

    @RequestMapping("/editCategories")
    public String editCategories(@RequestParam("id") Long id, ModelMap modelMap) {
        Categories categories = categoriesService.getCategories(id);
        modelMap.addAttribute("categories", categories);
        return "EditCategories";
    }

    @RequestMapping("/updateCategories")
    public String updateCategories(@ModelAttribute("categories") Categories categories) {
        categoriesService.updateCategories(categories);
        return "CreateCategories";
    }
}
