package com.veterinaire.formulaireveterinaire.service;

import com.veterinaire.formulaireveterinaire.entity.OurVeterinaire;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface OurVeterinaireService {
    void uploadExcel(MultipartFile file) throws Exception;
    List<OurVeterinaire> getAllVeterinaires();
}
