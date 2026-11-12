package com.example.miniecommerce.entities;

import jakarta.persistence.*;
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
public class Paniers {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Date date;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Utilisateurs utilisateurs;

    @OneToMany(mappedBy = "paniers" , fetch = FetchType.LAZY)
    @Builder.Default
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private List<LignePaniers> lignePaniers = new ArrayList<>();
}
