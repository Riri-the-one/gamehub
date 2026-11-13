package com.example.miniecommerce.services;

import com.example.miniecommerce.entities.Utilisateurs;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public interface UtilisateurService {
    Utilisateurs saveUtilisateurs(Utilisateurs utilisateurs);
    Utilisateurs updateUtilisateurs(Utilisateurs utilisateurs);
    void deleteUtilisateurs(Long id);
    Utilisateurs getUtilisateurs(Long id);
    void deleteAllUtilisateurs();
    List<Utilisateurs> getAllUtilisateurs();

    Utilisateurs checkAndGetCurrentUser(HttpSession session);
    Optional<Utilisateurs> recoverGuest(String token);
    void registerGuestToken(Long owner, String token);
}
