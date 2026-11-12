package com.example.miniecommerce;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.mock.web.MockHttpSession;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class AdminSecurityTests extends DatabaseTestSupport {
    private static final String PASSWORD = UUID.randomUUID().toString();
    @DynamicPropertySource
    static void isolatedSettings(DynamicPropertyRegistry registry) {
        registry.add("gamehub.admin.email", () -> "security-test@example.invalid");
        registry.add("gamehub.admin.password.hash", () -> new BCryptPasswordEncoder(4).encode(PASSWORD));
    }

    @Autowired WebApplicationContext context;
    @Autowired com.example.miniecommerce.repositories.CategoriesRepositorie categories;
    @Autowired com.example.miniecommerce.repositories.ProduitsRepositorie products;
    private MockMvc mvc;
    @BeforeEach void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/produits", "/panier", "/checkout", "/admin/login", "/css/style.css"})
    void publicPagesRemainAvailable(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/admin", "/admin/add-product", "/admin/add-category", "/utilisateursList", "/produitsList"})
    void anonymousCannotReadAdministration(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/login"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/admin/save-product", "/admin/update-product", "/admin/delete-product",
        "/admin/save-category", "/admin/delete-category", "/saveProduits", "/updateProduits",
        "/deleteProduits", "/saveCategories", "/saveUtilisateurs", "/deleteUtilisateurs",
        "/saveCommandes", "/savePaniers", "/savePaiements", "/saveLigneCommandes", "/saveLignePaniers"})
    void anonymousCannotWriteEvenWithCsrf(String path) throws Exception {
        mvc.perform(post(path).with(csrf())).andExpect(status().is3xxRedirection());
    }

    @Test void clientRoleCannotAccessAdministration() throws Exception {
        mvc.perform(get("/admin").with(user("client").roles("CLIENT"))).andExpect(status().isForbidden());
        mvc.perform(post("/admin/save-product").with(user("client").roles("CLIENT")).with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test void oldSessionFlagDoesNotGrantAccess() throws Exception {
        var session = new MockHttpSession();
        session.setAttribute("isAdmin", true);
        mvc.perform(get("/admin").session(session)).andExpect(status().is3xxRedirection());
    }

    @Test void adminPagesRenderCsrfProtectedForms() throws Exception {
        mvc.perform(get("/admin").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(get("/admin/add-product").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(get("/admin/login")).andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test void adminCanCreateAndDeleteCategoryWithCsrf() throws Exception {
        mvc.perform(post("/admin/save-category").with(user("admin").roles("ADMIN")).with(csrf())
                .param("nom", "Security test"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin"));
        Long categoryId = categories.findAll().stream().filter(c -> "Security test".equals(c.getNom()))
            .findFirst().orElseThrow().getId();
        mvc.perform(post("/admin/delete-category").with(user("admin").roles("ADMIN")).with(csrf())
                .param("id", categoryId.toString()))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin"));
        org.junit.jupiter.api.Assertions.assertFalse(categories.existsById(categoryId));
    }

    @Test void adminCanManageProductAndVisitorCanSeeIt() throws Exception {
        mvc.perform(post("/admin/save-product").with(user("admin").roles("ADMIN")).with(csrf())
                .param("nom", "Security test product").param("prix", "25").param("quantite", "2"))
            .andExpect(redirectedUrl("/admin"));
        Long id = products.findAll().stream().filter(p -> "Security test product".equals(p.getNom()))
            .findFirst().orElseThrow().getId();
        mvc.perform(get("/produits").param("search", "Security test product"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Security test product")))
            .andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(get("/admin/edit-product").param("id", id.toString()).with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk());
        mvc.perform(post("/admin/update-product").with(user("admin").roles("ADMIN")).with(csrf())
                .param("id", id.toString()).param("version",products.findById(id).orElseThrow().getVersion().toString()).param("nom", "Updated security product").param("prix", "30").param("quantite", "3"))
            .andExpect(redirectedUrl("/admin"));
        org.junit.jupiter.api.Assertions.assertEquals("Updated security product", products.findById(id).orElseThrow().getNom());
        mvc.perform(get("/admin").with(user("admin").roles("ADMIN")))
            .andExpect(content().string(containsString("/admin/delete-product?id=" + id)));
        mvc.perform(post("/admin/delete-product").param("id", id.toString()).with(user("admin").roles("ADMIN")).with(csrf()))
            .andExpect(redirectedUrl("/admin"));
        org.junit.jupiter.api.Assertions.assertFalse(products.existsById(id));
    }

    @Test void writesRequireCsrfAndGetCannotDelete() throws Exception {
        mvc.perform(post("/admin/save-category").with(user("admin").roles("ADMIN")))
            .andExpect(status().isForbidden());
        mvc.perform(post("/admin/save-category").with(user("admin").roles("ADMIN")).with(csrf().useInvalidToken()))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/delete-product").with(user("admin").roles("ADMIN")))
            .andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/deleteProduits").with(user("admin").roles("ADMIN")))
            .andExpect(status().isForbidden());
        mvc.perform(post("/panier/ajouter")).andExpect(status().isForbidden());
    }

    @Test void configuredLoginAndLogoutWork() throws Exception {
        mvc.perform(post("/admin/login").with(csrf()).param("email", "security-test@example.invalid")
                .param("password", "invalid"))
            .andExpect(redirectedUrl("/admin/login?error"));
        mvc.perform(post("/admin/login").param("email", "security-test@example.invalid").param("password", PASSWORD))
            .andExpect(status().isForbidden());
        var result = mvc.perform(post("/admin/login").with(csrf()).param("email", "security-test@example.invalid")
                .param("password", PASSWORD)).andExpect(redirectedUrl("/admin")).andReturn();
        var session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(get("/admin").session(session)).andExpect(status().isOk());
        mvc.perform(post("/admin/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/logout").session(session).with(csrf()))
            .andExpect(redirectedUrl("/admin/login?logout"));
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection());
    }
}
