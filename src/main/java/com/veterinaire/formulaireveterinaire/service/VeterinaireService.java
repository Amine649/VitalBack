package com.veterinaire.formulaireveterinaire.service;
import com.veterinaire.formulaireveterinaire.DTO.UserDTO;
import com.veterinaire.formulaireveterinaire.Enums.SubscriptionType;
import com.veterinaire.formulaireveterinaire.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface VeterinaireService {
    String updateVeterinaireProfile(Long userId, MultipartFile image, SubscriptionType subscriptionType);

    UserDTO getVeterinaireById(Long userId);


    UserDTO getVeterinaireByEmail(String email);
}
