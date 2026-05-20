package com.example.miniecommerce.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Produits {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nom;
    private String description;
    private Double prix;
    private Integer quantite;
    private String image;

    @ManyToOne
    private Categories categorie;

    @OneToMany(mappedBy = "produits", fetch = FetchType.LAZY)
    private List<LignePaniers> lignePaniers = new ArrayList<>();

    @OneToMany(mappedBy = "produit", fetch = FetchType.LAZY)
    private List<LigneCommandes> ligneCommandes = new ArrayList<>();

}
