package com.veterinaire.formulaireveterinaire.serviceimpl;

import com.veterinaire.formulaireveterinaire.DTO.SubscriptionDTO;
import com.veterinaire.formulaireveterinaire.DTO.UserDTO;
import com.veterinaire.formulaireveterinaire.Enums.SubscriptionType;
import com.veterinaire.formulaireveterinaire.DAO.UserRepository;
import com.veterinaire.formulaireveterinaire.entity.Subscription;
import com.veterinaire.formulaireveterinaire.entity.User;
import com.veterinaire.formulaireveterinaire.entity.VeterinaireProfile;
import com.veterinaire.formulaireveterinaire.service.VeterinaireService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;


@Service
public class VeterinaireServiceImpl implements VeterinaireService {
    private static final Logger logger = LoggerFactory.getLogger(VeterinaireServiceImpl.class);
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;


    public VeterinaireServiceImpl(UserRepository userRepository, JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.mailSender = mailSender;

    }

    @Value("${finance.email}")
    private String financeEmail;

    @Value("${app.upload.ordonnance-dir}")
    private String uploadDir;

    @Override
    public String updateVeterinaireProfile(Long userId, MultipartFile image, SubscriptionType subscriptionType) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID : " + userId));

        logger.info("Updating veterinary profile for user ID: {}", userId);

        VeterinaireProfile profile = user.getVeterinaireProfile();
        if (profile == null) {
            profile = new VeterinaireProfile();
            profile.setUser(user);
            user.setVeterinaireProfile(profile);
        }

        if (image != null && !image.isEmpty()) {
            try {
                // ✅ Clean filename
                String originalName = image.getOriginalFilename() != null ? image.getOriginalFilename() : "file";
                String safeName = originalName.replaceAll("\\s+", "_");

                String tel = user.getTelephone() != null ? user.getTelephone() : "user";
                String fileName = tel + "_" + System.currentTimeMillis() + "_" + safeName;

                // ✅ Directory
                String uploadDir = System.getProperty("user.dir")
                        + File.separator + "uploads"
                        + File.separator + "Ordonnance";

                File directory = new File(uploadDir);
                if (!directory.exists()) {
                    directory.mkdirs();
                }

                // ✅ FULL FILE PATH (THIS WAS YOUR MAIN BUG)
                String filePath = uploadDir + File.separator + fileName;

                image.transferTo(new File(filePath));

                // ✅ Save filename only
                profile.setImagePath(fileName);

                logger.info("Image saved at {} for user ID: {}", filePath, userId);

            } catch (IOException e) {
                logger.error("Error saving image for user ID {}: {}", userId, e.getMessage());
                throw new RuntimeException("Erreur lors de l'enregistrement de l'image", e);
            }
        }

        if (subscriptionType != null) {
            profile.setSubscriptionType(subscriptionType);
            logger.info("Subscription type {} set for user ID: {}", subscriptionType, userId);
        }

        userRepository.save(user);

        // ✅ avoid NullPointerException
        if (subscriptionType != null) {
            sendSubscriptionEmail(user.getEmail(), user.getNom(), subscriptionType.name(), financeEmail);
        }

        return "Profil vétérinaire mis à jour avec succès pour l'utilisateur ID " + userId + ".";
    }


    // ✅ Nouvelle méthode getById
    @Override
    public UserDTO getVeterinaireById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Vétérinaire non trouvé avec l'ID : " + userId));

        return mapToDTO(user);
    }

    public UserDTO getVeterinaireByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return mapToDTO(user); // whatever mapping you already use
    }


    // ✅ Méthode de mapping vers ton DTO
    private UserDTO mapToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setNom(user.getNom());
        dto.setPrenom(user.getPrenom());
        dto.setEmail(user.getEmail());
        dto.setTelephone(user.getTelephone());
        dto.setAdresseCabinet(user.getAdresseCabinet());
        dto.setNumMatricule(user.getNumMatricule());
        dto.setStatus(user.getStatus().name());

        if (user.getSubscription() != null) {
            Subscription sub = user.getSubscription();
            SubscriptionDTO subDto = new SubscriptionDTO();
            subDto.setId(sub.getId());
            subDto.setSubscriptionType(sub.getSubscriptionType().name());
            subDto.setStartDate(sub.getStartDate());
            subDto.setEndDate(sub.getEndDate());
            dto.setSubscription(subDto);
        }

        return dto;
    }




    private void sendSubscriptionEmail(String to, String nom, String subscriptionType, String financeEmail) {
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setCc(financeEmail);
            helper.setSubject("Confirmation de votre abonnement – VITALFEED");

            String duree;
            switch (subscriptionType) {
                case "ONE_MONTH":
                    duree = "1 mois";
                    break;
                case "THREE_MONTHS":
                    duree = "3 mois";
                    break;
                case "SIX_MONTHS":
                    duree = "6 mois";
                    break;
                default:
                    duree = "abonnement inconnu";
            }

            String htmlContent = """
            <html>
            <body style="margin:0; padding:0; background-color:#f4f6f8; font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif; color:#333;">
                <table align="center" width="100%%" cellpadding="0" cellspacing="0" style="max-width:650px; margin:auto; background-color:#ffffff; border-radius:10px; overflow:hidden; box-shadow:0 4px 10px rgba(0,0,0,0.05);">
                    <!-- Header -->
                    <tr>
                        <td style="background-color:#00897B; padding:25px 40px; text-align:center;">
                            <h1 style="margin:0; color:#ffffff; font-size:24px; letter-spacing:0.5px;">VITALFEED</h1>
                            <p style="color:#dff9f3; margin:5px 0 0; font-size:14px;">Simplifier Votre Quotidien Professionnel</p>
                        </td>
                    </tr>

                    <!-- Body -->
                    <tr>
                        <td style="padding:40px;">
                                <h2 style="color:#2c3e50; font-size:12px; font-weight:600; margin:0 0 20px 0; line-height:1.4;">
                                            Contactez notre service financier pour finaliser votre abonnement
                                </h2>
                                <p style="font-size:15px; line-height:1.6;">
                                Bonjour Dr <strong>%s</strong>,<br><br>
                                Nous Avons Bien Reçu Votre Demande D’abonnement Au Plan <strong>%s</strong> sur la plateforme <strong>VitalNutri\s</strong> De La Société <stong>VITALFEED</stong>.
                            </p>

                            <!-- Next Steps -->
                            <div style="margin-top:25px;">
                                <h3 style="color:#00897B; font-size:17px; border-bottom:2px solid #eaf0f6; padding-bottom:6px;">Prochaine étape :</h3>
                                <p style="font-size:15px; line-height:1.6;">
                                    Afin de finaliser votre inscription, veuillez nous confirmer le paiement via :
                                </p>
                                <p style="font-size:16px; text-align:center; margin:20px 0;">
                                    <a href="mailto:%s" style="color:#00897B; font-weight:bold; text-decoration:none;">vitalnutri@vitalfeed.com.tn\s</a>
                                </p>
                                <p style="font-size:14px; color:#777;">
                                    Un email Vous Sera Transmis Après Validations Et Vous Informons Que Votre Compte Est Désormais Activé
                                </p>
                            </div>

                            <div style="margin-top:35px;">
                                <p style="font-size:15px;">Nous vous remercions pour votre confiance et sommes ravis de vous compter parmi les vétérinaires utilisateurs de <strong>VitalNutri</strong> 🐾</p>
                                <p style="margin-top:20px; font-weight:600;">Bien cordialement,</p>
                                <p style="margin-top:5px; color:#00897B; font-weight:700;">L’équipe VITALFEED</p>
                            </div>
                        </td>
                    </tr>

                    <!-- Footer -->
                    <tr>
                        <td style="background-color:#f0f3f7; padding:15px 30px; text-align:center; font-size:12px; color:#777;">
                            Cet e-mail a été envoyé automatiquement, merci de ne pas y répondre directement.<br>
                            © %s VITALFEED – Tous droits réservés.
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(
                    nom,                   // Dr [Nom]
                    duree,                 // Duration (1 mois, 3 mois, etc.)
                    financeEmail,
                    financeEmail,
                    String.valueOf(LocalDate.now().getYear())
            );

            helper.setText(htmlContent, true);
            mailSender.send(message);

            logger.info("Subscription confirmation email sent to {}", to);
        } catch (MessagingException e) {
            logger.error("Failed to send subscription email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Erreur lors de l'envoi de l'e-mail de confirmation d'abonnement", e);
        }
    }

}


