package com.example.miniecommerce;

import com.example.miniecommerce.entities.*;
import com.example.miniecommerce.enums.*;
import com.example.miniecommerce.repositories.*;
import com.example.miniecommerce.services.*;
import com.example.miniecommerce.config.ReservationExpiryJob;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class FinalQualityTests extends DatabaseTestSupport {
    @Autowired WebApplicationContext context;
    @Autowired GuestCartService carts;
    @Autowired GuestOrderService guestOrders;
    @Autowired CommandesService orders;
    @Autowired ProduitsService productService;
    @Autowired ProduitsRepositorie products;
    @Autowired UtilisateurService users;
    @Autowired UtilisateursRepositorie userRepository;
    @Autowired CommandesRepositorie orderRepository;
    @Autowired PaiementsRepositorie payments;
    MockMvc mvc; Produits product;
    static final CheckoutDetails DETAILS=new CheckoutDetails("Guest","Test","guest@example.invalid","Adresse fictive");
    @BeforeEach void setup() {
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        product=new Produits();product.setNom("Final test "+UUID.randomUUID());product.setPrix(new BigDecimal("0.10"));
        product.setQuantite(10);product=products.saveAndFlush(product);
    }
    MockHttpSession session(){return new MockHttpSession();}
    Commandes order(MockHttpSession session) {
        carts.add(session,product.getId(),3);
        return guestOrders.submit(session,guestOrders.checkoutToken(session),DETAILS);
    }
    Produits copy() {
        var source=products.findById(product.getId()).orElseThrow();
        var value=new Produits();value.setId(source.getId());value.setVersion(source.getVersion());value.setNom(source.getNom());
        value.setPrix(source.getPrix());value.setQuantite(source.getQuantite());return value;
    }

    @Test void moneyUsesExactDecimalArithmeticAndConsistentRounding() {
        assertEquals(new BigDecimal("0.30"),Money.amount(new BigDecimal("0.10").multiply(BigDecimal.valueOf(3))));
        assertEquals(new BigDecimal("1.01"),Money.amount(new BigDecimal("1.005")));
        assertThrows(ResponseStatusException.class,()->Money.price(new BigDecimal("1.001")));
        assertThrows(ResponseStatusException.class,()->Money.amount(new BigDecimal("-1")));
        assertThrows(ResponseStatusException.class,()->Money.amount(new BigDecimal("100000000000000000")));
    }
    @Test void cartOrderAndPaymentHaveIdenticalDecimalTotals() {
        var a=session();carts.add(a,product.getId(),3);
        Commandes order=guestOrders.submit(a,guestOrders.checkoutToken(a),DETAILS);guestOrders.pay(a,order.getId(),"CASH");
        assertEquals(new BigDecimal("0.30"),order.getMontant());
        assertEquals(order.getMontant(),payments.findByCommandes_Id(order.getId()).orElseThrow().getMontant());
    }
    @Test void staleAdministratorFormCannotOverwriteCheckoutStock() {
        var stale=copy();order(session());
        var error=assertThrows(ResponseStatusException.class,()->productService.updateProduits(stale));
        assertEquals(409,error.getStatusCode().value());assertEquals(7,products.findById(product.getId()).orElseThrow().getQuantite());
    }
    @Test void competingAdministratorFormsAllowOnlyOneUpdate() throws Exception {
        var a=copy();var b=copy();a.setQuantite(12);b.setQuantite(14);
        var executor=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try {
            var futures=new ArrayList<Future<Boolean>>();
            for(var value:List.of(a,b))futures.add(executor.submit(()->{start.await();try{productService.updateProduits(value);return true;}
                catch(ResponseStatusException conflict){assertEquals(409,conflict.getStatusCode().value());return false;}}));
            start.countDown();int successes=0;for(var future:futures)if(future.get(30,TimeUnit.SECONDS))successes++;
            assertEquals(1,successes);
        }finally{executor.shutdownNow();}
    }
    @Test void administratorPricePrecisionAndQuantityAreValidated() {
        var value=copy();value.setPrix(new BigDecimal("1.001"));assertThrows(ResponseStatusException.class,()->productService.updateProduits(value));
        value.setPrix(new BigDecimal("1.00"));value.setQuantite(-1);assertThrows(ResponseStatusException.class,()->productService.updateProduits(value));
    }
    @Test void durableReplayWorksInANewSessionWithRecoveryCookie() throws Exception {
        var a=session();var first=mvc.perform(get("/").session(a)).andExpect(status().isOk()).andReturn();
        var proof=first.getResponse().getCookie("GAMEHUB_GUEST");assertNotNull(proof);assertTrue(proof.isHttpOnly());
        carts.add(a,product.getId(),1);String token=guestOrders.checkoutToken(a);Commandes order=guestOrders.submit(a,token,DETAILS);
        a.invalidate();var b=session();
        mvc.perform(get("/panier").session(b).cookie(proof)).andExpect(status().isOk());
        assertEquals(order.getId(),guestOrders.submit(b,token,DETAILS).getId());
        assertEquals(9,products.findById(product.getId()).orElseThrow().getQuantite());
    }
    @Test void replayRejectsChangedPayloadAndUnknownVisitor() {
        var a=session();carts.add(a,product.getId(),1);String token=guestOrders.checkoutToken(a);guestOrders.submit(a,token,DETAILS);
        var conflict=assertThrows(ResponseStatusException.class,()->guestOrders.submit(a,token,
            new CheckoutDetails("Other","Test","guest@example.invalid","Autre adresse")));
        assertEquals(409,conflict.getStatusCode().value());
        var missing=assertThrows(ResponseStatusException.class,()->guestOrders.submit(session(),token,DETAILS));
        assertEquals(404,missing.getStatusCode().value());
    }
    @Test void malformedRecoveryCookieCannotAdoptAnExistingGuest() throws Exception {
        var a=session();var existing=users.checkAndGetCurrentUser(a).getId();var b=session();
        mvc.perform(get("/").session(b).cookie(new jakarta.servlet.http.Cookie("GAMEHUB_GUEST","1"))).andExpect(status().isOk());
        assertNotEquals(existing,users.checkAndGetCurrentUser(b).getId());
    }
    @Test void explicitLogoutRevokesTheGuestRecoveryProof() throws Exception {
        var a=session();var initial=mvc.perform(get("/").session(a)).andReturn();
        var proof=initial.getResponse().getCookie("GAMEHUB_GUEST");Long original=users.checkAndGetCurrentUser(a).getId();
        mvc.perform(post("/admin/logout").session(a).cookie(proof).with(csrf())).andExpect(status().is3xxRedirection());
        var b=session();mvc.perform(get("/").session(b).cookie(proof)).andExpect(status().isOk());
        assertNotEquals(original,users.checkAndGetCurrentUser(b).getId());
    }
    @Test void expiredGuestProofCannotBeReusedAfterSessionLoss() throws Exception {
        var a=session();var initial=mvc.perform(get("/").session(a)).andReturn();
        var proof=initial.getResponse().getCookie("GAMEHUB_GUEST");var guest=users.checkAndGetCurrentUser(a);
        Long original=guest.getId();guest.setGuestTokenExpiresAt(new Date(0));userRepository.saveAndFlush(guest);
        var b=session();mvc.perform(get("/").session(b).cookie(proof)).andExpect(status().isOk());
        assertNotEquals(original,users.checkAndGetCurrentUser(b).getId());
    }
    @Test void pendingCancellationRestoresStockExactlyOnce() {
        var a=session();var order=order(a);guestOrders.cancel(a,order.getId());guestOrders.cancel(a,order.getId());
        assertEquals(10,products.findById(product.getId()).orElseThrow().getQuantite());
        assertEquals(CommandeEnum.ANNULEE,orderRepository.findById(order.getId()).orElseThrow().getCommandestatut());
        assertThrows(ResponseStatusException.class,()->guestOrders.pay(a,order.getId(),"CASH"));
    }
    @Test void paidOrderCannotBeCancelledAndForeignCancellationIs404() {
        var a=session();var order=order(a);guestOrders.pay(a,order.getId(),"CASH");
        assertEquals(409,assertThrows(ResponseStatusException.class,()->guestOrders.cancel(a,order.getId())).getStatusCode().value());
        assertEquals(404,assertThrows(ResponseStatusException.class,()->guestOrders.cancel(session(),order.getId())).getStatusCode().value());
        assertEquals(7,products.findById(product.getId()).orElseThrow().getQuantite());
    }
    @Test void optionalExpiryOnlyReleasesNewUnpaidReservations() {
        var a=session();var order=order(a);order.setDateCommande(new Date(0));orderRepository.saveAndFlush(order);
        new ReservationExpiryJob(orderRepository,orders,30).sweep();
        assertEquals(CommandeEnum.ANNULEE,orderRepository.findById(order.getId()).orElseThrow().getCommandestatut());
        assertEquals(10,products.findById(product.getId()).orElseThrow().getQuantite());
    }
    @Test void optionalExpiryDoesNotRewriteHistoricalOrders() {
        var a=session();var order=order(a);order.setCheckoutKey(null);order.setDateCommande(new Date(0));orderRepository.saveAndFlush(order);
        new ReservationExpiryJob(orderRepository,orders,30).sweep();
        assertEquals(CommandeEnum.EN_ATTENTE,orderRepository.findById(order.getId()).orElseThrow().getCommandestatut());
        assertEquals(7,products.findById(product.getId()).orElseThrow().getQuantite());
    }
    @RepeatedTest(3) void paymentRacingCancellationNeverReleasesPaidStock() throws Exception {
        var a=session();var order=order(a);var executor=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try {
            var pay=executor.submit(()->{start.await();try{guestOrders.pay(a,order.getId(),"CASH");}catch(ResponseStatusException conflict){assertEquals(409,conflict.getStatusCode().value());}return true;});
            var cancel=executor.submit(()->{start.await();try{guestOrders.cancel(a,order.getId());}catch(ResponseStatusException conflict){assertEquals(409,conflict.getStatusCode().value());}return true;});
            start.countDown();pay.get(30,TimeUnit.SECONDS);cancel.get(30,TimeUnit.SECONDS);
            var result=orderRepository.findById(order.getId()).orElseThrow();
            if(result.getCommandestatut()==CommandeEnum.CONFIRMEE) {
                assertTrue(payments.findByCommandes_Id(order.getId()).isPresent());assertEquals(7,products.findById(product.getId()).orElseThrow().getQuantite());
            }else{
                assertEquals(CommandeEnum.ANNULEE,result.getCommandestatut());assertTrue(payments.findByCommandes_Id(order.getId()).isEmpty());
                assertEquals(10,products.findById(product.getId()).orElseThrow().getQuantite());
            }
        }finally{executor.shutdownNow();}
    }
    @Test void cancellationRequiresCsrfAndLegacyFinancialWritesAreDenied() throws Exception {
        var a=session();var order=order(a);
        mvc.perform(post("/commander/annuler").session(a).param("commandeId",order.getId().toString())).andExpect(status().isForbidden());
        mvc.perform(post("/saveCommandes").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/savePaiements").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isForbidden());
    }
}
