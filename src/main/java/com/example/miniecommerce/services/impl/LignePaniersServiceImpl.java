package com.example.miniecommerce.services.impl;

import com.example.miniecommerce.entities.LignePaniers;
import com.example.miniecommerce.repositories.LignePaniersRepositorie;
import com.example.miniecommerce.services.LignePaniersService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class LignePaniersServiceImpl implements LignePaniersService {

    private LignePaniersRepositorie lignePaniersRepositorie;



    @Override
    public LignePaniers saveLignePaniers(LignePaniers lignePaniers) {
        return lignePaniersRepositorie.save(lignePaniers);
    }

    @Override
    public LignePaniers updateLignePaniers(LignePaniers lignePaniers) {
        return lignePaniersRepositorie.save(lignePaniers);
    }

    @Override
    public void deleteLignePaniers(Long id) {
        lignePaniersRepositorie.deleteById(id);
    }

    @Override
    public LignePaniers getLignePaniers(Long id) {
        return lignePaniersRepositorie.findById(id).orElse(null);
    }

    @Override
    public void deleteAllLignePaniers() {
        lignePaniersRepositorie.deleteAll();
    }

    @Override
    public List<LignePaniers> getAllLignePaniers() {
        return lignePaniersRepositorie.findAll();
    }

}
