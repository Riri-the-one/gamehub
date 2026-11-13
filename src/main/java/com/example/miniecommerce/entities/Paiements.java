package com.example.miniecommerce.entities;

import com.example.miniecommerce.enums.PaiementEnum;
import com.example.miniecommerce.enums.StatutPaiementEnum;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    @Column(precision = 19, scale = 2)
    private BigDecimal montant;
    private Date datePaiement;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id")
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Commandes commandes;

}
