package com.example.miniecommerce.entities;

import com.example.miniecommerce.enums.CommandeEnum;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Commandes {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 64, unique = true)
    @lombok.ToString.Exclude
    private String checkoutKey;
    @Column(length = 64)
    @lombok.ToString.Exclude
    private String checkoutFingerprint;
    private Date dateCommande;
    private CommandeEnum commandestatut;
    @Column(precision = 19, scale = 2)
    private BigDecimal montant;
    private String adresseDeLivraison;

    @ManyToOne
    @JoinColumn
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Utilisateurs utilisateur;

    @OneToMany(mappedBy = "commandes" , fetch = FetchType.LAZY)
    @Builder.Default
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private List<LigneCommandes> ligneCommandes = new ArrayList<>();

    @OneToOne(mappedBy = "commandes" , fetch = FetchType.LAZY)
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Paiements paiements;
}
