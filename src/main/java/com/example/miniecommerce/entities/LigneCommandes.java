package com.example.miniecommerce.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LigneCommandes {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer quantite;
    @Column(precision = 19, scale = 2)
    private BigDecimal prixUnitaire;

    @ManyToOne
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Produits produit;

    @ManyToOne
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Commandes commandes;
}
