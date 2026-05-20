package com.example.miniecommerce.controllers;

import com.example.miniecommerce.entities.Paiements;
import com.example.miniecommerce.services.PaiementService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@AllArgsConstructor
public class PaiementsController {
    private PaiementService paiementService;

    @RequestMapping("/createPaiements")
    public String createPaiements() {
        return "CreatePaiements";
    }

    @RequestMapping("/savePaiements")
    public String savePaiements(@ModelAttribute("paiements") Paiements paiements) {
        paiementService.savePaiements(paiements);
        return "CreatePaiements";
    }

    @RequestMapping("/paiementsList")
    public String paiementsList(ModelMap modelMap) {
        List<Paiements> paiements = paiementService.getAllPaiements();
        modelMap.addAttribute("paiements", paiements);
        return "PaiementsList";
    }

    @RequestMapping("/deletePaiements")
    public String deletePaiements(@RequestParam("id") Long id, ModelMap modelMap) {
        paiementService.deletePaiements(id);
        List<Paiements> paiements = paiementService.getAllPaiements();
        modelMap.addAttribute("paiements", paiements);
        return "PaiementsList";
    }

    @RequestMapping("/editPaiements")
    public String editPaiements(@RequestParam("id") Long id, ModelMap modelMap) {
        Paiements paiements = paiementService.getPaiements(id);
        modelMap.addAttribute("paiements", paiements);
        return "EditPaiements";
    }

    @RequestMapping("/updatePaiements")
    public String updatePaiements(@ModelAttribute("paiements") Paiements paiements) {
        paiementService.updatePaiements(paiements);
        return "CreatePaiements";
    }
}
