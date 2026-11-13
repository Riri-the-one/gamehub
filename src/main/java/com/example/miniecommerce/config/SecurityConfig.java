package com.example.miniecommerce.config;

import com.example.miniecommerce.services.UtilisateurService;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean
    UserDetailsService adminUsers(
            @Value("${gamehub.admin.email:}") String email,
            @Value("${gamehub.admin.password.hash:}") String hash) {
        var users = new InMemoryUserDetailsManager();
        if (!email.isBlank() && hash.matches("\\$2[aby]\\$(?:0[4-9]|[12][0-9]|3[01])\\$[./A-Za-z0-9]{53}")) {
            users.createUser(User.withUsername(email).password(hash).roles("ADMIN").build());
        }
        return users;
    }

    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, UtilisateurService guestUsers,
            @Value("${server.servlet.session.cookie.secure:false}") boolean secureCookies) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/admin/login", "/css/**", "/images/**", "/favicon.ico").permitAll()
                .requestMatchers("/create*", "/save*", "/edit*", "/update*", "/delete*",
                        "/categoriesList", "/produitsList", "/utilisateursList",
                        "/commandesList", "/ligneCommandesList", "/paniersList",
                        "/lignePaniersList", "/paiementsList").denyAll()
                .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/", "/produits", "/panier", "/checkout",
                        "/panier/supprimer/**", "/panier/vider", "/paiement/**", "/confirmation/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/panier/ajouter", "/panier/supprimer/**", "/panier/vider",
                        "/commander", "/commander/payer", "/commander/annuler").permitAll()
                .anyRequest().denyAll())
            .formLogin(login -> login.loginPage("/admin/login")
                .usernameParameter("email").defaultSuccessUrl("/admin", true)
                .failureUrl("/admin/login?error").permitAll())
            .logout(logout -> logout.logoutUrl("/admin/logout")
                .addLogoutHandler((request, response, authentication) -> {
                    var session=request.getSession(false);
                    if(session!=null && session.getAttribute(com.example.miniecommerce.services.impl.UtilisateurServiceImpl.GUEST_ID) instanceof Long owner) {
                        guestUsers.registerGuestToken(owner,com.example.miniecommerce.services.Secrets.token());
                    }
                })
                .deleteCookies(GuestIdentityFilter.COOKIE)
                .logoutSuccessUrl("/admin/login?logout"));
        // CSRF remains enabled for every unsafe request, including public forms.
        http.addFilterBefore(new GuestIdentityFilter(guestUsers, secureCookies),
                UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
