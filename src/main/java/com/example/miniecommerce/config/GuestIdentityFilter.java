package com.example.miniecommerce.config;

import com.example.miniecommerce.services.Secrets;
import com.example.miniecommerce.services.UtilisateurService;
import com.example.miniecommerce.services.impl.UtilisateurServiceImpl;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Arrays;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

/** Recover a guest using a high-entropy HttpOnly cookie, never a customer ID/email. */
public class GuestIdentityFilter extends OncePerRequestFilter {
    public static final String COOKIE = "GAMEHUB_GUEST";
    private final UtilisateurService users;
    private final boolean forceSecure;
    public GuestIdentityFilter(UtilisateurService users, boolean forceSecure) { this.users=users; this.forceSecure=forceSecure; }

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path=request.getRequestURI().substring(request.getContextPath().length());
        return !(path.equals("/") || path.startsWith("/produits") || path.startsWith("/panier")
                || path.startsWith("/checkout") || path.startsWith("/commander") || path.startsWith("/paiement/")
                || path.startsWith("/confirmation/"));
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        HttpSession session=request.getSession();
        synchronized (WebUtils.getSessionMutex(session)) {
            String token=request.getCookies()==null ? null : Arrays.stream(request.getCookies())
                    .filter(c->COOKIE.equals(c.getName())).map(Cookie::getValue).findFirst().orElse(null);
            var recovered=users.recoverGuest(token);
            if (session.getAttribute(UtilisateurServiceImpl.GUEST_ID)==null && recovered.isPresent()) {
                session.setAttribute(UtilisateurServiceImpl.GUEST_ID,recovered.get().getId());
            }
            var current=users.checkAndGetCurrentUser(session);
            if (recovered.isEmpty() || !recovered.get().getId().equals(current.getId())) {
                token=Secrets.token(); users.registerGuestToken(current.getId(),token);
                Cookie cookie=new Cookie(COOKIE,token); cookie.setPath("/");cookie.setHttpOnly(true);
                cookie.setSecure(forceSecure || request.isSecure());cookie.setMaxAge(7*24*60*60);cookie.setAttribute("SameSite","Lax");
                response.addCookie(cookie);
            }
        }
        chain.doFilter(request,response);
    }
}
