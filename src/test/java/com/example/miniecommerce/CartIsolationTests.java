package com.example.miniecommerce;

import com.example.miniecommerce.entities.Produits;
import com.example.miniecommerce.repositories.*;
import com.example.miniecommerce.services.*;
import com.example.miniecommerce.services.impl.UtilisateurServiceImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class CartIsolationTests extends DatabaseTestSupport {
    @Autowired WebApplicationContext context;
    @Autowired GuestCartService carts;
    @Autowired UtilisateurService users;
    @Autowired ProduitsRepositorie products;
    @Autowired PaniersRepositorie cartRepository;
    @Autowired LignePaniersRepositorie lines;
    @Autowired UtilisateursRepositorie userRepository;
    MockMvc mvc;
    Produits product;
    @BeforeEach void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        product = new Produits();
        product.setNom("Cart isolation " + UUID.randomUUID());
        product.setPrix(new java.math.BigDecimal("10.0")); product.setQuantite(200);
        product = products.save(product);
    }
    MockHttpSession session() { return new MockHttpSession(); }
    Long owner(MockHttpSession session) { return users.checkAndGetCurrentUser(session).getId(); }
    Long cartId(MockHttpSession session) { return carts.view(session).getId(); }
    void add(MockHttpSession session, String quantity) throws Exception {
        mvc.perform(post("/panier/ajouter").session(session).with(csrf())
                .param("produitId", product.getId().toString()).param("quantite", quantity))
            .andExpect(redirectedUrl("/panier"));
    }

    @Test void separateSessionsAndSameSessionContinuity() throws Exception {
        var a=session(); var b=session();
        assertNotEquals(owner(a),owner(b)); assertNotEquals(cartId(a),cartId(b));
        Long original=cartId(a); add(a,"2");
        mvc.perform(get("/panier").session(a)).andExpect(status().isOk())
            .andExpect(content().string(containsString(product.getNom())));
        mvc.perform(get("/panier").session(b)).andExpect(status().isOk())
            .andExpect(content().string(not(containsString(product.getNom()))));
        mvc.perform(get("/panier").session(b).param("panierId",original.toString()).param("utilisateurId",owner(a).toString()))
            .andExpect(status().isOk()).andExpect(content().string(not(containsString(product.getNom()))));
        add(b,"1"); assertEquals(original,cartId(a));
        assertEquals(2,lines.findByPaniers_Id(original).get(0).getQuantite());
        assertEquals(1,lines.findByPaniers_Id(cartId(b)).get(0).getQuantite());
    }

    @Test void foreignLineCannotBeDeletedEvenWithValidCsrf() throws Exception {
        var a=session(); var b=session(); add(a,"1");
        Long lineId=lines.findByPaniers_Id(cartId(a)).get(0).getId();
        mvc.perform(post("/panier/supprimer/"+lineId).session(b).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post("/panier/supprimer/9223372036854775807").session(b).with(csrf())).andExpect(status().isNotFound());
        assertTrue(lines.existsById(lineId));
        mvc.perform(post("/panier/supprimer/"+lineId).session(a).with(csrf())).andExpect(redirectedUrl("/panier"));
        assertFalse(lines.existsById(lineId));
    }

    @Test void injectedIdsAndClearingCannotTargetAnotherOwner() throws Exception {
        var a=session(); var b=session(); add(a,"2");
        mvc.perform(post("/panier/ajouter").session(b).with(csrf()).param("produitId",product.getId().toString())
                .param("quantite","1").param("utilisateurId",owner(a).toString()).param("panierId",cartId(a).toString()))
            .andExpect(redirectedUrl("/panier"));
        mvc.perform(post("/panier/vider").session(b).with(csrf()).param("panierId",cartId(a).toString()))
            .andExpect(redirectedUrl("/panier"));
        assertEquals(2,lines.findByPaniers_Id(cartId(a)).get(0).getQuantite());
        assertTrue(lines.findByPaniers_Id(cartId(b)).isEmpty());
    }

    @ParameterizedTest @ValueSource(strings={"0","-1","100","2147483647","2147483648","abc",""})
    void invalidQuantitiesCannotWrite(String quantity) throws Exception {
        var a=session(); Long id=cartId(a);
        mvc.perform(post("/panier/ajouter").session(a).with(csrf()).param("produitId",product.getId().toString())
                .param("quantite",quantity)).andExpect(status().isBadRequest());
        assertTrue(lines.findByPaniers_Id(id).isEmpty());
    }

    @Test void accumulatedLimitAndStockAreValidatedWithoutPartialWrites() throws Exception {
        var a=session(); add(a,"98");
        mvc.perform(post("/panier/ajouter").session(a).with(csrf()).param("produitId",product.getId().toString())
            .param("quantite","2")).andExpect(status().isBadRequest());
        assertEquals(98,lines.findByPaniers_Id(cartId(a)).get(0).getQuantite());
        var b=session(); product.setQuantite(1); products.save(product);
        mvc.perform(post("/panier/ajouter").session(b).with(csrf()).param("produitId",product.getId().toString())
            .param("quantite","2")).andExpect(status().isBadRequest());
        assertTrue(lines.findByPaniers_Id(cartId(b)).isEmpty());
    }

    @Test void productMustExistAndDefaultQuantityStillWorks() throws Exception {
        var a=session();
        mvc.perform(post("/panier/ajouter").session(a).with(csrf()).param("produitId","9223372036854775807"))
            .andExpect(status().isNotFound());
        mvc.perform(post("/panier/ajouter").session(a).with(csrf()).param("produitId",product.getId().toString()))
            .andExpect(redirectedUrl("/panier"));
        assertEquals(1,lines.findByPaniers_Id(cartId(a)).get(0).getQuantite());
    }

    @Test void csrfAndPostOnlyEnforced() throws Exception {
        var a=session(); add(a,"1");
        Long line=lines.findByPaniers_Id(cartId(a)).get(0).getId();
        for(String path:List.of("/panier/ajouter","/panier/vider","/panier/supprimer/"+line)) {
            mvc.perform(post(path).session(a)).andExpect(status().isForbidden());
            mvc.perform(post(path).session(a).with(csrf().useInvalidToken())).andExpect(status().isForbidden());
        }
        mvc.perform(get("/panier/vider").session(a)).andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/panier/supprimer/"+line).session(a)).andExpect(status().isMethodNotAllowed());
        assertTrue(lines.existsById(line));
        mvc.perform(get("/panier").session(a)).andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test void oldSharedIdentityIsNotAdoptedAndCheckoutStillRenders() throws Exception {
        var a=session(); var b=session();
        var shared=users.checkAndGetCurrentUser(a);
        b.setAttribute("currentUser",shared);
        assertNotEquals(shared.getId(),owner(b));
        a.setAttribute("currentUser",users.checkAndGetCurrentUser(b));
        assertEquals(shared.getId(),owner(a));
        add(a,"1");
        mvc.perform(get("/checkout").session(a)).andExpect(status().isOk())
            .andExpect(content().string(containsString(product.getNom())));
        a.invalidate(); assertNotEquals(shared.getId(),owner(session()));
    }

    void parallel(int count, Callable<Void> operation) throws Exception {
        var executor=Executors.newFixedThreadPool(count);
        var start=new CountDownLatch(1);
        try {
            var futures=new ArrayList<Future<Void>>();
            for(int i=0;i<count;i++) futures.add(executor.submit(()->{start.await(); return operation.call();}));
            start.countDown();
            for(var future:futures) future.get(30,TimeUnit.SECONDS);
        } finally { executor.shutdownNow(); }
    }

    @Test void concurrentFirstRequestsCreateOneIdentityAndCart() throws Exception {
        var a=session(); long before=userRepository.count();
        parallel(8,()->{carts.view(a); return null;});
        assertEquals(before+1,userRepository.count());
        assertTrue(cartRepository.findByUtilisateurs_Id(owner(a)).isPresent());
    }

    @Test void concurrentAdditionsMergeWithoutLosingQuantities() throws Exception {
        var a=session(); owner(a);
        parallel(8,()->{carts.add(a,product.getId(),1);return null;});
        var actual=lines.findByPaniers_Id(cartId(a));
        assertEquals(1,actual.size()); assertEquals(8,actual.get(0).getQuantite());
    }

    @Test void concurrentLimitChecksDoNotPermitOverflow() throws Exception {
        var a=session(); add(a,"98");
        var accepted=new java.util.concurrent.atomic.AtomicInteger();
        parallel(8,()->{
            try { carts.add(a,product.getId(),1); accepted.incrementAndGet(); }
            catch(org.springframework.web.server.ResponseStatusException error) {
                assertEquals(400,error.getStatusCode().value());
            }
            return null;
        });
        assertEquals(1,accepted.get());
        assertEquals(99,lines.findByPaniers_Id(cartId(a)).get(0).getQuantite());
    }

    @Test void clearAndAddAreSerializedAndCannotDuplicateLines() throws Exception {
        var a=session(); add(a,"1");
        var operation=new java.util.concurrent.atomic.AtomicInteger();
        parallel(8,()->{
            if(operation.incrementAndGet()%2==0) carts.clear(a);
            else carts.add(a,product.getId(),1);
            return null;
        });
        var actual=lines.findByPaniers_Id(cartId(a));
        assertTrue(actual.size()<=1);
        if(!actual.isEmpty()) assertTrue(actual.get(0).getQuantite()>=1 && actual.get(0).getQuantite()<=4);
    }

    @Test void absentStockAndNullQuantityAreRejectedByTheService() {
        var a=session();
        var invalid=assertThrows(org.springframework.web.server.ResponseStatusException.class,
            ()->carts.add(a,product.getId(),null));
        assertEquals(400,invalid.getStatusCode().value());
        product.setQuantite(null); products.save(product);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->carts.add(a,product.getId(),1));
        assertTrue(lines.findByPaniers_Id(cartId(a)).isEmpty());
    }
}
