package com.example.miniecommerce.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LignePaniers {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer quantite;

    @ManyToOne
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Paniers paniers;

    @ManyToOne
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Produits produits;
}
