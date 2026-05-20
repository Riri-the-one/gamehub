package com.example.miniecommerce.services;

import com.example.miniecommerce.entities.Paiements;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public interface PaiementService {
    Paiements savePaiements(Paiements paiements);
    Paiements updatePaiements(Paiements paiements);
    void deletePaiements(Long id);
    Paiements getPaiements(Long id);
    void deleteAllPaiements();
    List<Paiements> getAllPaiements();
}
