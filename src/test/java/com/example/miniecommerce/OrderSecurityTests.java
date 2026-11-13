package com.example.miniecommerce;

import com.example.miniecommerce.entities.*;
import com.example.miniecommerce.enums.*;
import com.example.miniecommerce.repositories.*;
import com.example.miniecommerce.services.*;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class OrderSecurityTests extends DatabaseTestSupport {
    @Autowired WebApplicationContext context;
    @Autowired GuestCartService carts;
    @Autowired GuestOrderService guestOrders;
    @Autowired CommandesService orders;
    @Autowired UtilisateurService users;
    @Autowired ProduitsRepositorie products;
    @Autowired CommandesRepositorie orderRepository;
    @Autowired PaniersRepositorie cartRepository;
    @MockitoSpyBean LignePaniersRepositorie cartLines;
    @Autowired UtilisateursRepositorie userRepository;
    @MockitoSpyBean LigneCommandesRepositorie orderLines;
    @MockitoSpyBean PaiementsRepositorie payments;
    @Autowired EntityManager entityManager;
    MockMvc mvc;
    Produits product;
    static final CheckoutDetails DETAILS = new CheckoutDetails("Test", "Invité", "guest@example.invalid", "Adresse fictive de test");

    @BeforeEach void setup() {
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        product=new Produits(); product.setNom("Order test "+UUID.randomUUID());
        product.setPrix(new java.math.BigDecimal("12.5")); product.setQuantite(10); product=products.save(product);
    }
    MockHttpSession session() { return new MockHttpSession(); }
    Long owner(MockHttpSession session) {return users.checkAndGetCurrentUser(session).getId();}
    Commandes create(MockHttpSession session) {
        carts.add(session,product.getId(),1);
        return guestOrders.submit(session,guestOrders.checkoutToken(session),DETAILS);
    }
    void expectStatus(int expected, Runnable operation) {
        assertEquals(expected,assertThrows(ResponseStatusException.class,operation::run).getStatusCode().value());
    }
    long ownedCount(Long id) {
        return orderRepository.findAll().stream().filter(o->o.getUtilisateur()!=null&&id.equals(o.getUtilisateur().getId())).count();
    }
    List<Long> parallel(int count, Callable<Long> operation) throws Exception {
        var executor=Executors.newFixedThreadPool(count);var start=new CountDownLatch(1);
        try {
            var futures=new ArrayList<Future<Long>>();
            for(int i=0;i<count;i++)futures.add(executor.submit(()->{start.await();return operation.call();}));
            start.countDown();var result=new ArrayList<Long>();
            for(var future:futures)result.add(future.get(30,TimeUnit.SECONDS));
            return result;
        }finally{executor.shutdownNow();}
    }

    @Test void orderCreationCommitsAllResourcesAndSnapshotsPrice() {
        var a=session();carts.add(a,product.getId(),2);
        Commandes order=guestOrders.submit(a,guestOrders.checkoutToken(a),DETAILS);
        assertEquals(new java.math.BigDecimal("25.0").setScale(2),order.getMontant());assertEquals(CommandeEnum.EN_ATTENTE,order.getCommandestatut());
        assertEquals(8,products.findById(product.getId()).orElseThrow().getQuantite());
        assertTrue(cartLines.findByPaniers_Id(carts.view(a).getId()).isEmpty());
        assertEquals(DETAILS.nom(),userRepository.findById(owner(a)).orElseThrow().getNom());
        var line=orderLines.findAll().stream().filter(l->order.getId().equals(l.getCommandes().getId())).findFirst().orElseThrow();
        assertEquals(new java.math.BigDecimal("12.5").setScale(2),line.getPrixUnitaire());assertEquals(2,line.getQuantite());
        product=products.findById(product.getId()).orElseThrow(); product.setPrix(new java.math.BigDecimal("90.0"));products.save(product);
        assertEquals(new java.math.BigDecimal("25.0").setScale(2),guestOrders.view(a,order.getId()).getMontant());
    }

    @Test void foreignAndMissingOrdersReturn404OnAllPublicPaths() throws Exception {
        var a=session();var b=session();Commandes order=create(a);
        for(Long id:List.of(order.getId(),Long.MAX_VALUE)) {
            mvc.perform(get("/paiement/"+id).session(b)).andExpect(status().isNotFound());
            mvc.perform(get("/confirmation/"+id).session(b)).andExpect(status().isNotFound());
            mvc.perform(post("/commander/payer").session(b).with(csrf()).param("commandeId",id.toString())
                .param("methode","CASH")).andExpect(status().isNotFound());
        }
        assertTrue(payments.findByCommandes_Id(order.getId()).isEmpty());
        assertEquals(CommandeEnum.EN_ATTENTE,orderRepository.findById(order.getId()).orElseThrow().getCommandestatut());
    }

    @Test void historicalSharedUserCannotBeAdoptedForOrders() throws Exception {
        var old=session();var next=session();Commandes order=create(old);
        next.setAttribute("currentUser",users.checkAndGetCurrentUser(old));
        mvc.perform(get("/confirmation/"+order.getId()).session(next)).andExpect(status().isNotFound());
    }

    @Test void flushThenFailureRollsBackOrderStockLinesContactAndCart() {
        var a=session();Long id=owner(a);carts.add(a,product.getId(),2);
        long beforeOrders=ownedCount(id), beforeLines=orderLines.count();
        doAnswer(invocation->{entityManager.flush();throw new IllegalStateException("Injected persistence failure");})
            .when(cartLines).deleteAll(anyIterable());
        String token=guestOrders.checkoutToken(a);
        assertThrows(org.springframework.dao.InvalidDataAccessApiUsageException.class,()->guestOrders.submit(a,token,DETAILS));
        assertEquals(beforeOrders,ownedCount(id));assertEquals(beforeLines,orderLines.count());
        assertEquals(10,products.findById(product.getId()).orElseThrow().getQuantite());
        assertEquals("",userRepository.findById(id).orElseThrow().getNom());
        assertEquals(2,cartLines.findByPaniers_Id(carts.view(a).getId()).get(0).getQuantite());
        reset(cartLines);
        assertNotNull(guestOrders.submit(a,token,DETAILS).getId()); // Failed token remains retryable.
    }

    @Test void laterUnavailableProductLeavesNoPartialOrder() {
        var a=session();carts.add(a,product.getId(),1);
        var second=new Produits();second.setNom("Second "+UUID.randomUUID());second.setPrix(new java.math.BigDecimal("20.0"));second.setQuantite(1);
        second=products.save(second);carts.add(a,second.getId(),1);second.setQuantite(0);products.save(second);
        long before=ownedCount(owner(a));
        expectStatus(409,()->guestOrders.submit(a,guestOrders.checkoutToken(a),DETAILS));
        assertEquals(before,ownedCount(owner(a)));assertEquals(10,products.findById(product.getId()).orElseThrow().getQuantite());
        assertEquals(2,cartLines.findByPaniers_Id(carts.view(a).getId()).size());
    }

    @ParameterizedTest @ValueSource(strings={"negative","missing","overflow"})
    void invalidDatabasePriceCannotCreateAnOrder(String kind) {
        var a=session();carts.add(a,product.getId(),2);
        if (kind.equals("negative")) product.setPrix(new java.math.BigDecimal("-1.0"));
        else if (kind.equals("missing")) product.setPrix(null);
        else product.setPrix(new java.math.BigDecimal("99999999999999999.99"));
        products.save(product);
        expectStatus(409,()->guestOrders.submit(a,guestOrders.checkoutToken(a),DETAILS));
        assertEquals(0,ownedCount(owner(a)));assertEquals(10,products.findById(product.getId()).orElseThrow().getQuantite());
    }

    @Test void legacyInvalidCartQuantityIsRechecked() {
        var a=session();carts.add(a,product.getId(),1);
        var line=cartLines.findByPaniers_Id(carts.view(a).getId()).get(0);line.setQuantite(-2);cartLines.save(line);
        expectStatus(409,()->guestOrders.submit(a,guestOrders.checkoutToken(a),DETAILS));
        assertEquals(0,ownedCount(owner(a)));assertEquals(10,products.findById(product.getId()).orElseThrow().getQuantite());
    }

    @Test void competingOrdersForLastUnitHaveExactlyOneWinner() throws Exception {
        var a=session();var b=session();carts.add(a,product.getId(),1);carts.add(b,product.getId(),1);
        product.setQuantite(1);products.save(product);
        var owners=List.of(owner(a),owner(b));var index=new AtomicInteger();
        var result=parallel(2,()->{
            try{return orders.passerCommande(owners.get(index.getAndIncrement()),DETAILS).getId();}
            catch(ResponseStatusException error){assertEquals(409,error.getStatusCode().value());return -1L;}
        });
        assertEquals(1,result.stream().filter(id->id>0).count());
        assertEquals(0,products.findById(product.getId()).orElseThrow().getQuantite());
        assertEquals(1,ownedCount(owners.get(0))+ownedCount(owners.get(1)));
    }

    @Test void repeatedAndConcurrentSessionSubmissionReturnsSameOrder() throws Exception {
        var a=session();carts.add(a,product.getId(),1);String token=guestOrders.checkoutToken(a);
        var ids=parallel(4,()->guestOrders.submit(a,token,DETAILS).getId());
        assertEquals(1,new HashSet<>(ids).size());assertEquals(1,ownedCount(owner(a)));
        assertEquals(9,products.findById(product.getId()).orElseThrow().getQuantite());
        carts.add(a,product.getId(),1);assertEquals(ids.get(0),guestOrders.submit(a,token,DETAILS).getId());
        assertEquals(1,cartLines.findByPaniers_Id(carts.view(a).getId()).get(0).getQuantite());
    }

    @Test void directConcurrentSameCartSubmissionsDoNotDuplicateOrders() throws Exception {
        var a=session();carts.add(a,product.getId(),1);Long id=owner(a);
        var result=parallel(2,()->{try{return orders.passerCommande(id,DETAILS).getId();}
            catch(ResponseStatusException error){assertEquals(409,error.getStatusCode().value());return -1L;}});
        assertEquals(1,result.stream().filter(value->value>0).count());assertEquals(1,ownedCount(id));
    }

    @Test void concurrentClearAndCheckoutRemainConsistent() throws Exception {
        var a=session();carts.add(a,product.getId(),1);Long id=owner(a);var turn=new AtomicInteger();
        parallel(2,()->{
            if(turn.getAndIncrement()==0){carts.clear(a);return 0L;}
            try{return orders.passerCommande(id,DETAILS).getId();}
            catch(ResponseStatusException error){assertEquals(409,error.getStatusCode().value());return -1L;}
        });
        long count=ownedCount(id);assertTrue(count==0||count==1);
        assertEquals(10-count,products.findById(product.getId()).orElseThrow().getQuantite().longValue());
        assertTrue(cartLines.findByPaniers_Id(carts.view(a).getId()).isEmpty());
    }

    @Test void repeatedAndConcurrentPaymentsCreateOneRecordAndDoNotDebitStockAgain() throws Exception {
        var a=session();Commandes order=create(a);Long id=owner(a);
        parallel(4,()->{orders.validerPaiementCommande(id,order.getId(),"CASH");return order.getId();});
        assertEquals(1,payments.findAll().stream().filter(p->order.getId().equals(p.getCommandes().getId())).count());
        assertEquals(CommandeEnum.CONFIRMEE,orderRepository.findById(order.getId()).orElseThrow().getCommandestatut());
        assertEquals(9,products.findById(product.getId()).orElseThrow().getQuantite());
        expectStatus(409,()->guestOrders.pay(a,order.getId(),"PAYPAL"));
    }

    @Test void invalidPaymentMethodHasNoSideEffects() {
        var a=session();Commandes order=create(a);
        expectStatus(400,()->guestOrders.pay(a,order.getId(),"INVALID"));
        assertEquals(CommandeEnum.EN_ATTENTE,orderRepository.findById(order.getId()).orElseThrow().getCommandestatut());
        assertTrue(payments.findByCommandes_Id(order.getId()).isEmpty());
    }

    @Test void paymentPersistenceFailureRollsBackConfirmation() {
        var a=session();Commandes order=create(a);
        doAnswer(invocation->{entityManager.flush();throw new IllegalStateException("Injected payment failure");})
            .when(payments).save(any(Paiements.class));
        assertThrows(org.springframework.dao.InvalidDataAccessApiUsageException.class,()->guestOrders.pay(a,order.getId(),"CASH"));
        assertEquals(CommandeEnum.EN_ATTENTE,orderRepository.findById(order.getId()).orElseThrow().getCommandestatut());
        assertTrue(payments.findByCommandes_Id(order.getId()).isEmpty());
    }

    @Test void cancelledOrderCannotBePaid() {
        var a=session();Commandes order=create(a);order.setCommandestatut(CommandeEnum.ANNULEE);orderRepository.save(order);
        expectStatus(409,()->guestOrders.pay(a,order.getId(),"CASH"));assertTrue(payments.findByCommandes_Id(order.getId()).isEmpty());
    }

    @Test void confirmationReflectsPendingAndPaidState() throws Exception {
        var a=session();Commandes order=create(a);
        mvc.perform(get("/confirmation/"+order.getId()).session(a)).andExpect(status().isOk())
            .andExpect(content().string(containsString("En attente de paiement")));
        guestOrders.pay(a,order.getId(),"CASH");
        mvc.perform(get("/confirmation/"+order.getId()).session(a)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Confirmée")))
            .andExpect(content().string(containsString("Paiement simulé validé")));
    }

    @Test void creationAndPaymentRequireCsrf() throws Exception {
        var a=session();Commandes order=create(a);
        for(String path:List.of("/commander","/commander/payer")) {
            mvc.perform(post(path).session(a)).andExpect(status().isForbidden());
            mvc.perform(post(path).session(a).with(csrf().useInvalidToken())).andExpect(status().isForbidden());
        }
        mvc.perform(post("/commander/payer").session(a).with(csrf()).param("commandeId",order.getId().toString())
            .param("methode","CASH")).andExpect(redirectedUrl("/confirmation/"+order.getId()));
    }

    @Test void browserAmountOwnerAndStatusFieldsCannotOverrideServerValues() throws Exception {
        var a=session();carts.add(a,product.getId(),2);String token=guestOrders.checkoutToken(a);
        mvc.perform(post("/commander").session(a).with(csrf()).param("checkoutToken",token)
            .param("prenom",DETAILS.prenom()).param("nom",DETAILS.nom()).param("email",DETAILS.email()).param("adresse",DETAILS.adresse())
            .param("montant","0").param("prix","0").param("utilisateurId","1").param("commandestatut","CONFIRMEE"))
            .andExpect(status().is3xxRedirection());
        var order=orderRepository.findAll().stream().filter(o->owner(a).equals(o.getUtilisateur().getId())).findFirst().orElseThrow();
        assertEquals(new java.math.BigDecimal("25.0").setScale(2),order.getMontant());assertEquals(CommandeEnum.EN_ATTENTE,order.getCommandestatut());
    }

    @Test void missingOrCrossSessionSubmissionTokenAndInvalidDetailsDoNotWrite() {
        var a=session();var b=session();carts.add(a,product.getId(),1);
        expectStatus(409,()->guestOrders.submit(a,null,DETAILS));
        expectStatus(409,()->guestOrders.submit(b,guestOrders.checkoutToken(a),DETAILS));
        expectStatus(400,()->guestOrders.submit(a,guestOrders.checkoutToken(a),new CheckoutDetails("", "", "invalid", "")));
        assertEquals(0,ownedCount(owner(a)));assertEquals(10,products.findById(product.getId()).orElseThrow().getQuantite());
    }
}
