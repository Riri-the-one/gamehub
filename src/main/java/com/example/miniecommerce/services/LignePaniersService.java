package com.example.miniecommerce.services;

import com.example.miniecommerce.entities.LignePaniers;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public interface LignePaniersService {
    LignePaniers saveLignePaniers(LignePaniers lignePaniers);
    LignePaniers updateLignePaniers(LignePaniers lignePaniers);
    void deleteLignePaniers(Long id);
    LignePaniers getLignePaniers(Long id);
    void deleteAllLignePaniers();
    List<LignePaniers> getAllLignePaniers();
}
