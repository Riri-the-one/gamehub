package com.example.miniecommerce.services.impl;

import com.example.miniecommerce.entities.Utilisateurs;
import com.example.miniecommerce.enums.UtilisateurRoleEnum;
import com.example.miniecommerce.repositories.UtilisateursRepositorie;
import com.example.miniecommerce.services.PaniersService;
import com.example.miniecommerce.services.UtilisateurService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class UtilisateurServiceImpl implements UtilisateurService {

    private UtilisateursRepositorie utilisateursRepositorie;
    private PaniersService paniersService;

    @Override
    public Utilisateurs saveUtilisateurs(Utilisateurs utilisateurs) {
        return utilisateursRepositorie.save(utilisateurs);
    }

    @Override
    public Utilisateurs updateUtilisateurs(Utilisateurs utilisateurs) {
        return utilisateursRepositorie.save(utilisateurs);
    }

    @Override
    public void deleteUtilisateurs(Long id) {
        utilisateursRepositorie.deleteById(id);
    }

    @Override
    public Utilisateurs getUtilisateurs(Long id) {
        return utilisateursRepositorie.findById(id).orElse(null);
    }

    @Override
    public void deleteAllUtilisateurs() {
        utilisateursRepositorie.deleteAll();
    }

    @Override
    public List<Utilisateurs> getAllUtilisateurs() {
        return utilisateursRepositorie.findAll();
    }

    @Override
    public Utilisateurs getOrCreateDefaultClient() {
        Utilisateurs user = null;
        List<Utilisateurs> tous = utilisateursRepositorie.findAll();
        for (Utilisateurs u : tous) {
            if (u.getUtilisateurRole() == UtilisateurRoleEnum.Client) {
                user = u;
                if ("".equals(user.getNom())) {
                    user.setNom("");
                    user.setPrenom("");
                    user.setEmail("");
                    user = utilisateursRepositorie.save(user);
                }
                break;
            }
        }
        if (user == null) {
            user = Utilisateurs.builder()
                    .nom("")
                    .prenom("")
                    .email("")
                    .UtilisateurRole(UtilisateurRoleEnum.Client)
                    .build();
            user = utilisateursRepositorie.save(user);
        }
        paniersService.getOrCreatePanierForUtilisateur(user.getId());
        return user;
    }

    @Override
    public Utilisateurs loginAdmin(String email) {
        for (Utilisateurs u : utilisateursRepositorie.findAll()) {
            if (u.getUtilisateurRole() == UtilisateurRoleEnum.Administrateur && u.getEmail() != null && u.getEmail().equalsIgnoreCase(email)) {
                return u;
            }
        }
        return null;
    }

    @Override
    public Utilisateurs checkAndGetCurrentUser(jakarta.servlet.http.HttpSession session) {
        Utilisateurs user = (Utilisateurs) session.getAttribute("currentUser");
        if (user == null) {
            user = getOrCreateDefaultClient();
            session.setAttribute("currentUser", user);
        }
        return user;
    }

    @Override
    public boolean isAdmin(jakarta.servlet.http.HttpSession session) {
        return session.getAttribute("isAdmin") != null;
    }

    @Override
    public boolean authenticateAdmin(String email, String password, jakarta.servlet.http.HttpSession session) {
        Utilisateurs admin = loginAdmin(email);
        if (admin != null && "admin".equals(password)) {
            session.setAttribute("isAdmin", true);
            return true;
        }
        return false;
    }

    @Override
    public void logoutAdmin(jakarta.servlet.http.HttpSession session) {
        session.removeAttribute("isAdmin");
    }

    @Override
    public String handleAdminLogin(String email, String password, jakarta.servlet.http.HttpSession session, org.springframework.ui.Model model) {
        if (authenticateAdmin(email, password, session)) {
            return "redirect:/admin";
        }
        model.addAttribute("error", "Identifiant administrateur ou mot de passe incorrect !");
        return "admin_login";
    }
}
