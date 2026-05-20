package com.example.miniecommerce.services.impl;

import com.example.miniecommerce.entities.Paiements;
import com.example.miniecommerce.repositories.PaiementsRepositorie;
import com.example.miniecommerce.services.PaiementService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@AllArgsConstructor
public class PaiementServiceImpl implements PaiementService {

    private PaiementsRepositorie paiementsRepositorie;

    @Override
    public Paiements savePaiements(Paiements paiements) {
        return paiementsRepositorie.save(paiements);
    }

    @Override
    public Paiements updatePaiements(Paiements paiements) {
        return paiementsRepositorie.save(paiements);
    }

    @Override
    public void deletePaiements(Long id) {
        paiementsRepositorie.deleteById(id);
    }

    @Override
    public Paiements getPaiements(Long id) {
        return paiementsRepositorie.findById(id).orElse(null);
    }

    @Override
    public void deleteAllPaiements() {
        paiementsRepositorie.deleteAll();
    }

    @Override
    public List<Paiements> getAllPaiements() {
        return paiementsRepositorie.findAll();
    }
}
