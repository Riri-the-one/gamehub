package com.example.miniecommerce.services;

import com.example.miniecommerce.entities.Commandes;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.WebUtils;

@Service
@AllArgsConstructor
public class GuestOrderService {
    private static final String ATTEMPT = "gamehub.checkoutAttempt.v1";
    private final UtilisateurService users;
    private final CommandesService orders;

    private record Attempt(String token, Long completedOrder) implements Serializable { }

    public String checkoutToken(HttpSession session) {
        synchronized (WebUtils.getSessionMutex(session)) {
            Object current = session.getAttribute(ATTEMPT);
            if (current instanceof Attempt attempt && attempt.completedOrder() == null) return attempt.token();
            var attempt = new Attempt(Secrets.token(), null);
            session.setAttribute(ATTEMPT, attempt);
            return attempt.token();
        }
    }

    public Commandes submit(HttpSession session, String token, CheckoutDetails details) {
        synchronized (WebUtils.getSessionMutex(session)) {
            Long owner = users.checkAndGetCurrentUser(session).getId();
            if (token != null && token.matches("[0-9a-f]{64}")) {
                var previous = orders.replay(owner, token, details);
                if (previous.isPresent()) return previous.get();
            }
            Object current = session.getAttribute(ATTEMPT);
            if (!(current instanceof Attempt attempt) || !attempt.token().equals(token)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Rechargez la page de commande.");
            }
            if (attempt.completedOrder() != null) return orders.getOwnedCommande(owner, attempt.completedOrder());
            // The transactional proxy commits before returning. Session state is updated only after success.
            Commandes order = orders.passerCommande(owner, details, token);
            session.setAttribute(ATTEMPT, new Attempt(attempt.token(), order.getId()));
            users.checkAndGetCurrentUser(session);
            return order;
        }
    }

    public Commandes view(HttpSession session, Long id) {
        return orders.getOwnedCommande(users.checkAndGetCurrentUser(session).getId(), id);
    }

    public void pay(HttpSession session, Long id, String method) {
        orders.validerPaiementCommande(users.checkAndGetCurrentUser(session).getId(), id, method);
    }
    public void cancel(HttpSession session, Long id) {
        orders.cancel(users.checkAndGetCurrentUser(session).getId(), id);
    }
}
