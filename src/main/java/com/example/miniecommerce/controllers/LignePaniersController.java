package com.example.miniecommerce.controllers;

import com.example.miniecommerce.entities.LignePaniers;
import com.example.miniecommerce.services.LignePaniersService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@AllArgsConstructor
public class LignePaniersController {
    private LignePaniersService lignePaniersService;

    @RequestMapping("/createLignePaniers")
    public String createLignePaniers() {
        return "CreateLignePaniers";
    }

    @RequestMapping("/saveLignePaniers")
    public String saveLignePaniers(@ModelAttribute("lignePaniers") LignePaniers lignePaniers) {
        lignePaniersService.saveLignePaniers(lignePaniers);
        return "CreateLignePaniers";
    }

    @RequestMapping("/lignePaniersList")
    public String lignePaniersList(ModelMap modelMap) {
        List<LignePaniers> lignePaniers = lignePaniersService.getAllLignePaniers();
        modelMap.addAttribute("lignePaniers", lignePaniers);
        return "LignePaniersList";
    }

    @RequestMapping("/deleteLignePaniers")
    public String deleteLignePaniers(@RequestParam("id") Long id, ModelMap modelMap) {
        lignePaniersService.deleteLignePaniers(id);
        List<LignePaniers> lignePaniers = lignePaniersService.getAllLignePaniers();
        modelMap.addAttribute("lignePaniers", lignePaniers);
        return "LignePaniersList";
    }

    @RequestMapping("/editLignePaniers")
    public String editLignePaniers(@RequestParam("id") Long id, ModelMap modelMap) {
        LignePaniers lignePaniers = lignePaniersService.getLignePaniers(id);
        modelMap.addAttribute("lignePaniers", lignePaniers);
        return "EditLignePaniers";
    }

    @RequestMapping("/updateLignePaniers")
    public String updateLignePaniers(@ModelAttribute("lignePaniers") LignePaniers lignePaniers) {
        lignePaniersService.updateLignePaniers(lignePaniers);
        return "CreateLignePaniers";
    }
}
