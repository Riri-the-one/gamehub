package com.example.miniecommerce.services;

import com.example.miniecommerce.entities.Utilisateurs;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public interface UtilisateurService {
    Utilisateurs saveUtilisateurs(Utilisateurs utilisateurs);
    Utilisateurs updateUtilisateurs(Utilisateurs utilisateurs);
    void deleteUtilisateurs(Long id);
    Utilisateurs getUtilisateurs(Long id);
    void deleteAllUtilisateurs();
    List<Utilisateurs> getAllUtilisateurs();
    Utilisateurs getOrCreateDefaultClient();
    Utilisateurs loginAdmin(String email);
    Utilisateurs checkAndGetCurrentUser(jakarta.servlet.http.HttpSession session);
    boolean isAdmin(jakarta.servlet.http.HttpSession session);
    boolean authenticateAdmin(String email, String password, jakarta.servlet.http.HttpSession session);
    void logoutAdmin(jakarta.servlet.http.HttpSession session);
    String handleAdminLogin(String email, String password, jakarta.servlet.http.HttpSession session, org.springframework.ui.Model model);
}
