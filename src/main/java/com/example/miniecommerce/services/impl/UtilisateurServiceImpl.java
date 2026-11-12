package com.example.miniecommerce.services.impl;

import com.example.miniecommerce.entities.Utilisateurs;
import com.example.miniecommerce.enums.UtilisateurRoleEnum;
import com.example.miniecommerce.repositories.UtilisateursRepositorie;
import com.example.miniecommerce.services.Secrets;
import com.example.miniecommerce.services.UtilisateurService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.WebUtils;

@Service
@AllArgsConstructor
public class UtilisateurServiceImpl implements UtilisateurService {

    private UtilisateursRepositorie utilisateursRepositorie;
    public static final String GUEST_ID = "gamehub.guestUserId.v2";

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

    private Utilisateurs createGuestClient() {
        // Always create a new guest; never adopt a historical customer or shared cart.
        Utilisateurs user = new Utilisateurs();
        user.setNom("");
        user.setPrenom("");
        user.setEmail("");
        user.setUtilisateurRole(UtilisateurRoleEnum.Client);
        return utilisateursRepositorie.save(user);
    }

    @Override
    public Optional<Utilisateurs> recoverGuest(String token) {
        if (token == null || !token.matches("[0-9a-f]{64}")) return Optional.empty();
        return utilisateursRepositorie.findByGuestTokenHash(Secrets.hash(token))
                .filter(u -> u.getUtilisateurRole() == UtilisateurRoleEnum.Client
                        && u.getGuestTokenExpiresAt()!=null && u.getGuestTokenExpiresAt().after(new java.util.Date()));
    }

    @Override
    @Transactional
    public void registerGuestToken(Long owner, String token) {
        var user = utilisateursRepositorie.findLockedById(owner).orElseThrow();
        user.setGuestTokenHash(Secrets.hash(token));
        user.setGuestTokenExpiresAt(new java.util.Date(System.currentTimeMillis()+7L*24*60*60*1000));
    }

    @Override
    public Utilisateurs checkAndGetCurrentUser(HttpSession session) {
        synchronized (WebUtils.getSessionMutex(session)) {
            Object identity = session.getAttribute(GUEST_ID);
            Utilisateurs user = identity instanceof Long id
                    ? utilisateursRepositorie.findById(id).orElse(null) : null;
            if (user == null || user.getUtilisateurRole() != UtilisateurRoleEnum.Client) {
                user = createGuestClient();
                session.setAttribute(GUEST_ID, user.getId());
            }
            // Compatibility for checkout rendering; this attribute is never authoritative.
            session.setAttribute("currentUser", user);
            return user;
        }
    }
}
