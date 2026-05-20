package com.example.miniecommerce.entities;

import com.example.miniecommerce.enums.PaiementEnum;
import com.example.miniecommerce.enums.StatutPaiementEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Paiements {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private PaiementEnum MethodePaiement;
    private StatutPaiementEnum statutPaiement;
    private Double montant;
    private Date datePaiement;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id")
    private Commandes commandes;

}
