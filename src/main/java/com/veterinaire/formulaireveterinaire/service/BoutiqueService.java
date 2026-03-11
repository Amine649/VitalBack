package com.veterinaire.formulaireveterinaire.service;

import com.veterinaire.formulaireveterinaire.entity.Boutique;

import java.util.List;

public interface BoutiqueService {

    Boutique saveBoutique(Boutique boutique) throws Exception;

    Boutique updateBoutique(Long id, Boutique boutique) throws Exception;

    void deleteBoutique(Long id) throws Exception;

    List<Boutique> getAllBoutiques();
}
