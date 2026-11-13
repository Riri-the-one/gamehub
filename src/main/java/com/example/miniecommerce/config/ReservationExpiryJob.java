package com.example.miniecommerce.config;

import com.example.miniecommerce.enums.CommandeEnum;
import com.example.miniecommerce.repositories.CommandesRepositorie;
import com.example.miniecommerce.services.CommandesService;
import java.time.Instant;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Optional, bounded sweep; historical orders without a replay key are excluded. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name="gamehub.reservations.expiry-enabled",havingValue="true")
public class ReservationExpiryJob {
    private final CommandesRepositorie repository;
    private final CommandesService orders;
    private final long minutes;
    public ReservationExpiryJob(CommandesRepositorie repository,CommandesService orders,
            @Value("${gamehub.reservations.minutes:30}") long minutes) {
        if(minutes<1) throw new IllegalArgumentException("Reservation duration must be positive");
        this.repository=repository;this.orders=orders;this.minutes=minutes;
    }
    @Scheduled(fixedDelayString="${gamehub.reservations.sweep-ms:60000}")
    public void sweep() {
        Date cutoff=Date.from(Instant.now().minusSeconds(minutes*60));
        for(var order:repository.findTop100ByCommandestatutAndDateCommandeBeforeAndCheckoutKeyIsNotNull(CommandeEnum.EN_ATTENTE,cutoff)) {
            orders.expire(order.getUtilisateur().getId(),order.getId(),cutoff);
        }
    }
}
