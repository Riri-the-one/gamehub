package com.example.miniecommerce.services;

import com.example.miniecommerce.entities.Paniers;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/** HTTP-facing cart operations: ownership always comes from the server session. */
@Service
@AllArgsConstructor
public class GuestCartService {
    private final UtilisateurService users;
    private final PaniersService carts;

    public Paniers view(HttpSession session) {
        return carts.getOrCreatePanierForUtilisateur(users.checkAndGetCurrentUser(session).getId());
    }

    public void add(HttpSession session, Long productId, Integer quantity) {
        carts.addForUtilisateur(users.checkAndGetCurrentUser(session).getId(), productId, quantity);
    }

    public void remove(HttpSession session, Long lineId) {
        carts.removeForUtilisateur(users.checkAndGetCurrentUser(session).getId(), lineId);
    }

    public void clear(HttpSession session) {
        carts.clearForUtilisateur(users.checkAndGetCurrentUser(session).getId());
    }
}
