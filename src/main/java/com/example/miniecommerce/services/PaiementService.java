package com.example.miniecommerce.services;

import com.example.miniecommerce.entities.Paiements;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public interface PaiementService {
    Paiements savePaiements(Paiements paiements);
    Paiements updatePaiements(Paiements paiements);
    void deletePaiements(Long id);
    Paiements getPaiements(Long id);
    void deleteAllPaiements();
    List<Paiements> getAllPaiements();
}
