package com.example.miniecommerce.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
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
public class Produits {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    private Long version;
    private String nom;
    private String description;
    @Column(precision = 19, scale = 2)
    private BigDecimal prix;
    private Integer quantite;
    private String image;

    @ManyToOne
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Categories categorie;

    @OneToMany(mappedBy = "produits", fetch = FetchType.LAZY)
    @Builder.Default
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private List<LignePaniers> lignePaniers = new ArrayList<>();

    @OneToMany(mappedBy = "produit", fetch = FetchType.LAZY)
    @Builder.Default
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private List<LigneCommandes> ligneCommandes = new ArrayList<>();

}
