package com.veterinaire.formulaireveterinaire.service;

import com.veterinaire.formulaireveterinaire.entity.CabinetVeterinaire;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CabinetVeterinaireService {
    CabinetVeterinaire saveCabinet(CabinetVeterinaire cabinet) throws Exception;
    CabinetVeterinaire updateCabinet(Long id, CabinetVeterinaire cabinet) throws Exception;
    void deleteCabinet(Long id) throws Exception;
    List<CabinetVeterinaire> getAllCabinets();
}
