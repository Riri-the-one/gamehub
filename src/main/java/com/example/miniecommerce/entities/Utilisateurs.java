package com.example.miniecommerce.entities;

import com.example.miniecommerce.enums.UtilisateurRoleEnum;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Utilisateurs {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 64, unique = true)
    @lombok.ToString.Exclude
    private String guestTokenHash;
    private java.util.Date guestTokenExpiresAt;
    private String nom;
    private String prenom;
    private String email;
    private UtilisateurRoleEnum UtilisateurRole;

    @OneToMany(mappedBy ="utilisateur" , fetch = FetchType.LAZY)
    @Builder.Default
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private List<Commandes> commandes = new ArrayList<>();

    @OneToOne(mappedBy = "utilisateurs" , fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Paniers paniers;


}
