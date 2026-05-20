package com.example.miniecommerce.entities;

import com.example.miniecommerce.enums.CommandeEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Commandes {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Date dateCommande;
    private CommandeEnum commandestatut;
    private Double montant;
    private String adresseDeLivraison;

    @ManyToOne
    @JoinColumn
    private Utilisateurs utilisateur;

    @OneToMany(mappedBy = "commandes" , fetch = FetchType.LAZY)
    private List<LigneCommandes> ligneCommandes = new ArrayList<>();

    @OneToOne(mappedBy = "commandes" , fetch = FetchType.LAZY)
    private Paiements paiements;
}
