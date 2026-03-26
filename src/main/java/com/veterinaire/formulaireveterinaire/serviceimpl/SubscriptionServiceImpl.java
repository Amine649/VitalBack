package com.veterinaire.formulaireveterinaire.serviceimpl;

import com.veterinaire.formulaireveterinaire.Config.BrevoEmailService;
import com.veterinaire.formulaireveterinaire.Enums.SubscriptionStatus;
import com.veterinaire.formulaireveterinaire.Enums.SubscriptionType;
import com.veterinaire.formulaireveterinaire.DAO.SubscriptionRepository;
import com.veterinaire.formulaireveterinaire.DAO.UserRepository;
import com.veterinaire.formulaireveterinaire.DTO.SubscriptionDTO;
import com.veterinaire.formulaireveterinaire.DTO.UserDTO;
import com.veterinaire.formulaireveterinaire.entity.Subscription;
import com.veterinaire.formulaireveterinaire.entity.User;
import com.veterinaire.formulaireveterinaire.service.SubscriptionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionServiceImpl.class);
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final BrevoEmailService emailService;

    @Value("${finance.email}")
    private String financeEmail;

    public SubscriptionServiceImpl(UserRepository userRepository, SubscriptionRepository subscriptionRepository,
                                   BrevoEmailService emailService) {
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.emailService = emailService;

    }

    @Override
    public String assignSubscription(Long userId, SubscriptionType subscriptionType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID : " + userId));

        Subscription existingSubscription = user.getSubscription();
        if (existingSubscription != null) {
            logger.warn("User ID: {} already has a subscription with type: {}", userId, existingSubscription.getSubscriptionType());
            throw new RuntimeException("L'utilisateur avec l'ID " + userId + " a déjà un abonnement."); // Throw instead of return
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = calculateEndDate(now, subscriptionType);

        Subscription subscription = Subscription.builder()
                .user(user)
                .subscriptionType(subscriptionType)
                .startDate(now)
                .endDate(endDate)
                .build();
        user.setSubscription(subscription);
        subscriptionRepository.save(subscription);
        logger.info("Created new subscription for user ID: {} with type: {}", userId, subscriptionType);

        if (user.getStatus() != SubscriptionStatus.ACTIVE) {
            user.setStatus(SubscriptionStatus.ACTIVE);
            userRepository.save(user);
            logger.info("User ID: {} status updated to ACTIVE", userId);
        }

        sendSubscriptionEmail(user, subscriptionType, now, endDate, financeEmail);

        return "Abonnement assigné avec succès pour l'utilisateur ID " + userId + ". Vérifiez votre email.";
    }

    @Override
    public String updateSubscription(Long subscriptionId, SubscriptionType subscriptionType) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Abonnement non trouvé avec l'ID : " + subscriptionId));

        User user = subscription.getUser();
        if (user == null) {
            throw new RuntimeException("Utilisateur associé à l'abonnement non trouvé.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newEndDate = calculateEndDate(now, subscriptionType);

        subscription.setSubscriptionType(subscriptionType);
        subscription.setStartDate(now);
        subscription.setEndDate(newEndDate);
        subscriptionRepository.save(subscription);

        logger.info("Updated subscription ID: {} with new type: {} for user ID: {}", subscriptionId, subscriptionType, user.getId());

        sendSubscriptionUpdateEmail(user, subscriptionType, now, newEndDate, financeEmail);

        // Only success reaches here
        return "Abonnement mis à jour avec succès pour l'utilisateur ID " + user.getId() + ". Vérifiez votre email.";
    }

    @Override
    public String deleteSubscription(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Abonnement non trouvé avec l'ID : " + subscriptionId));

        User user = subscription.getUser();
        if (user == null) {
            throw new RuntimeException("Utilisateur associé à l'abonnement non trouvé.");
        }

        // Detach subscription from user before delete (avoids potential issues)
        user.setSubscription(null);

        subscriptionRepository.delete(subscription);
        logger.info("Deleted subscription ID: {} for user ID: {}", subscriptionId, user.getId());

        if (user.getStatus() == SubscriptionStatus.ACTIVE) {
            user.setStatus(SubscriptionStatus.INACTIVE);
            userRepository.save(user);
            logger.info("User ID: {} status updated to INACTIVE", user.getId());
        }

        // Optional: Add email sending here if needed
        // sendSubscriptionDeleteEmail(user, financeEmail);

        return "Abonnement supprimé avec succès pour l'utilisateur ID " + user.getId() + ".";
    }

    @Override
    public List<SubscriptionDTO> getAllSubscriptions() {
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        logger.debug("Retrieved {} subscriptions", subscriptions.size());
        return subscriptions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<SubscriptionDTO> getSubscriptionById(Long subscriptionId) {
        Optional<Subscription> subscription = subscriptionRepository.findById(subscriptionId);
        logger.debug("Retrieved subscription with ID: {} - Found: {}", subscriptionId, subscription.isPresent());
        return subscription.map(this::convertToDTO);
    }

    public SubscriptionDTO convertToDTO(Subscription subscription) {
        if (subscription == null) return null;

        UserDTO userDTO = new UserDTO();
        User user = subscription.getUser();
        userDTO.setId(user.getId());
        userDTO.setNom(user.getNom());
        userDTO.setPrenom(user.getPrenom());
        userDTO.setEmail(user.getEmail());
        userDTO.setTelephone(user.getTelephone());
        userDTO.setAdresseCabinet(user.getAdresseCabinet());
        userDTO.setNumMatricule(user.getNumMatricule());
        userDTO.setStatus(user.getStatus().name());

        SubscriptionDTO dto = new SubscriptionDTO();
        dto.setId(subscription.getId());
        dto.setUser(userDTO);
        dto.setSubscriptionType(subscription.getSubscriptionType().name());
        dto.setStartDate(subscription.getStartDate());
        dto.setEndDate(subscription.getEndDate());
        return dto;
    }

    private LocalDateTime calculateEndDate(LocalDateTime startDate, SubscriptionType subscriptionType) {
        switch (subscriptionType) {
            case ONE_MONTH:
                return startDate.plusMonths(1);
            case THREE_MONTHS:
                return startDate.plusMonths(3);
            case SIX_MONTHS:
                return startDate.plusMonths(6);
            default:
                throw new IllegalArgumentException("Type d'abonnement invalide");
        }
    }

    private void sendSubscriptionEmail(User user, SubscriptionType subscriptionType,
                                       LocalDateTime startDate, LocalDateTime endDate, String ccEmail) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String startDateStr = startDate.format(formatter);
        String endDateStr = endDate.format(formatter);

        try {
            String nom = user.getNom() != null ? user.getNom() : "Cher utilisateur";
            boolean isPlanned = startDate.isAfter(LocalDateTime.now());
            String subject = "Confirmation de votre Abonnement – VITALFEED";

            String htmlContent = """
        <html>
        <body style="margin:0; padding:0; background-color:#f4f6f8; font-family:'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color:#333;">
            <table align="center" width="100%%" cellpadding="0" cellspacing="0" style="max-width:650px; margin:auto; background-color:#ffffff; border-radius:10px; overflow:hidden; box-shadow:0 4px 10px rgba(0,0,0,0.05);">
                
                <tr>
                    <td style="background-color:#00897B; padding:25px 40px; text-align:center;">
                        <h1 style="margin:0; color:#ffffff; font-size:24px; letter-spacing:0.5px;">VITALFEED</h1>
                        <p style="color:#dff9f3; margin:5px 0 0; font-size:14px;">Simplifier Votre Quotidien Professionnel</p>
                    </td>
                </tr>

                <tr>
                    <td style="padding:40px;">
                        <h2 style="color:#2c3e50;">Confirmation de votre abonnement</h2>
                        <p style="font-size:15px; line-height:1.6;">
                            Bonjour Dr <strong>%s</strong>,<br><br>
                            %s sur <strong>VITALFEED</strong> a été %s avec succès.
                        </p>

                        <div style="margin-top:25px;">
                            <h3 style="color:#00897B; font-size:17px; border-bottom:2px solid #eaf0f6; padding-bottom:6px;">Détails de l’abonnement</h3>
                            <table width="100%%" cellpadding="0" cellspacing="0" style="margin-top:10px; border-collapse:collapse; font-size:14px;">
                                <tr>
                                    <td style="padding:8px; color:#555;">Type d’abonnement :</td>
                                    <td style="padding:8px; text-align:right; font-weight:600;">%s</td>
                                </tr>
                                <tr style="background-color:#f9fbfd;">
                                    <td style="padding:8px; color:#555;">Date de début :</td>
                                    <td style="padding:8px; text-align:right; font-weight:600;">%s</td>
                                </tr>
                                <tr>
                                    <td style="padding:8px; color:#555;">Date de fin :</td>
                                    <td style="padding:8px; text-align:right; font-weight:600;">%s</td>
                                </tr>
                            </table>
                        </div>

                        <div style="margin-top:35px;">
                            <p style="font-size:15px;">Merci pour votre confiance et bienvenue dans la communauté VITALFEED 🐾</p>
                            <p style="margin-top:20px; font-weight:600;">Bien cordialement,</p>
                            <p style="margin-top:5px; color:#00897B; font-weight:700;">L’équipe VITALFEED</p>
                        </div>
                    </td>
                </tr>

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
                    nom,
                    isPlanned ? "Votre abonnement a été planifié" : "Votre abonnement",
                    isPlanned ? "planifié" : "activé",
                    subscriptionType.name().replace("_", " "),
                    startDateStr,
                    endDateStr,
                    String.valueOf(LocalDate.now().getYear())
            );

            // Send to vet
            emailService.sendEmail(user.getEmail(), subject, htmlContent, ccEmail);

            logger.info("Subscription email sent to {} with CC to {}", user.getEmail(), ccEmail);
        } catch (Exception e) {
            logger.error("Failed to send subscription email to {}: {}", user.getEmail(), e.getMessage());
            throw new RuntimeException("Erreur lors de l'envoi de l'email de confirmation", e);
        }
    }



    private void sendSubscriptionUpdateEmail(User user, SubscriptionType subscriptionType,
                                             LocalDateTime startDate, LocalDateTime endDate, String ccEmail) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String startDateStr = startDate.format(formatter);
        String endDateStr = endDate.format(formatter);

        try {
            String nom = user.getNom() != null ? user.getNom() : "Cher utilisateur";
            String subject = "Confirmation de mise à jour de votre abonnement – VITALFEED";

            String htmlContent = """
        <html>
        <body style="margin:0; padding:0; background-color:#f4f6f8; font-family:'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color:#333;">
            <table align="center" width="100%%" cellpadding="0" cellspacing="0" style="max-width:650px; margin:auto; background-color:#ffffff; border-radius:10px; overflow:hidden; box-shadow:0 4px 10px rgba(0,0,0,0.05);">
                
                <tr>
                    <td style="background-color:#00897B; padding:25px 40px; text-align:center;">
                        <h1 style="margin:0; color:#ffffff; font-size:24px; letter-spacing:0.5px;">VITALFEED</h1>
                        <p style="color:#dff9f3; margin:5px 0 0; font-size:14px;">Simplifier Votre Quotidien Professionnel</p>
                    </td>
                </tr>

                <tr>
                    <td style="padding:40px;">
                    
                        <h2 style="color:#2c3e50; font-size:20px;">Bonjour Dr <span style="color:#00897B; font-weight:600;">%s</span>,</h2>
                        <p style="font-size:15px; line-height:1.6; margin-top:10px;">
                            Nous vous confirmons que votre <strong>abonnement</strong> à notre plateforme <strong>VITALFEED</strong> a été mis à jour avec succès.
                        </p>

                        <div style="margin-top:25px;">
                            <h3 style="font-size:17px; color:#00897B; font-weight:600; border-bottom:2px solid #eaf0f6; padding-bottom:6px;">Détails de l’abonnement</h3>
                            <table width="100%%" cellpadding="0" cellspacing="0" style="margin-top:10px; border-collapse:collapse; font-size:14px;">
                                <tr>
                                    <td style="padding:8px; color:#555;">Type d’abonnement :</td>
                                    <td style="padding:8px; text-align:right; font-weight:600;">%s</td>
                                </tr>
                                <tr style="background-color:#f9fbfd;">
                                    <td style="padding:8px; color:#555;">Date de début :</td>
                                    <td style="padding:8px; text-align:right; font-weight:600;">%s</td>
                                </tr>
                                <tr>
                                    <td style="padding:8px; color:#555;">Date de fin :</td>
                                    <td style="padding:8px; text-align:right; font-weight:600;">%s</td>
                                </tr>
                            </table>
                        </div>

                        <div style="margin-top:25px; font-size:15px; line-height:1.6;">
                            <p>Pour toute question relative à la facturation ou au paiement, notre service financier est disponible à :</p>
                            <p style="margin-top:8px;">
                                <a href="mailto:%s" style="color:#00897B; font-weight:600; text-decoration:none;">%s</a>
                            </p>
                        </div>

                        <div style="margin-top:35px;">
                            <p style="font-size:15px;">Nous vous remercions de votre confiance et restons à votre disposition pour toute assistance complémentaire.</p>
                            <p style="margin-top:20px; font-weight:600;">Bien cordialement,</p>
                            <p style="margin-top:5px; color:#00897B; font-weight:700;">L’équipe VITALFEED</p>
                        </div>
                    </td>
                </tr>

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
                    nom,
                    subscriptionType.name().replace("_", " "),
                    startDateStr,
                    endDateStr,
                    ccEmail,
                    ccEmail,
                    String.valueOf(LocalDate.now().getYear())
            );

            // Send to vet
            emailService.sendEmail(user.getEmail(), subject, htmlContent, ccEmail);

            logger.info("Subscription update email sent to {} with CC to {}", user.getEmail(), ccEmail);
        } catch (Exception e) {
            logger.error("Failed to send subscription update email to {}: {}", user.getEmail(), e.getMessage());
            throw new RuntimeException("Erreur lors de l'envoi de l'e-mail de mise à jour", e);
        }
    }





}