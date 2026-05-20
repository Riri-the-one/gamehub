package com.example.miniecommerce.entities;

import com.example.miniecommerce.enums.UtilisateurRoleEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Utilisateurs {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private UtilisateurRoleEnum UtilisateurRole;

    @OneToMany(mappedBy ="utilisateur" , fetch = FetchType.LAZY)
    private List<Commandes> commandes = new ArrayList<>();

    @OneToOne(mappedBy = "utilisateurs" , fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Paniers paniers;


}
