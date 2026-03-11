package com.veterinaire.formulaireveterinaire.Schedule;

import com.veterinaire.formulaireveterinaire.Enums.SubscriptionStatus;
import com.veterinaire.formulaireveterinaire.DAO.SubscriptionRepository;
import com.veterinaire.formulaireveterinaire.DAO.UserRepository;
import com.veterinaire.formulaireveterinaire.entity.Subscription;
import com.veterinaire.formulaireveterinaire.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class ReminderScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    private static final Logger logger = LoggerFactory.getLogger(ReminderScheduler.class);

    // In-memory Set to track subscriptions for which reminders have been sent
    private final Set<Long> sentReminderIds = new HashSet<>();

    public ReminderScheduler(SubscriptionRepository subscriptionRepository, UserRepository userRepository, JavaMailSender mailSender) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
    }

    @Value("${finance.email}")
    private String financeEmail;

   //@Scheduled(cron = "0 * * * * *") // Run every minute (for testing)
   @Scheduled(cron = "0 0 0 * * *") // Run daily at midnight
    public void checkExpiredSubscriptions() {
        logger.info("Checking for subscription reminders at {}", LocalDateTime.now());
        List<User> users = userRepository.findAllByStatus(SubscriptionStatus.ACTIVE);

        if (users == null || users.isEmpty()) {
            logger.info("No active users found to check for reminders.");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneWeekFromNow = now.plusDays(7);

        for (User user : users) {
            Subscription subscription = subscriptionRepository.findByUserEmail(user.getEmail()).orElse(null);
            if (subscription == null) {
                logger.warn("No subscription found for active user {} ({})", user.getPrenom(), user.getEmail());
                continue;
            }

            Long subscriptionId = subscription.getId();
            LocalDateTime endDate = subscription.getEndDate();

            if (endDate == null) {
                logger.warn("Subscription ID {} has no end date, skipping reminder check.", subscriptionId);
                continue;
            }

            if (endDate.isBefore(now)) {
                logger.info("Subscription ID {} for user {} ({}) has expired.", subscriptionId, user.getPrenom(), user.getEmail());
                sentReminderIds.remove(subscriptionId); // Remove from set if expired
                // Optionally update status to INACTIVE here if desired
                // user.setStatus(SubscriptionStatus.INACTIVE);
                // userRepository.save(user);
            } else if (endDate.isBefore(oneWeekFromNow) && !sentReminderIds.contains(subscriptionId)) {
                logger.info("Sending reminder for subscription ID {} to user {} ({})", subscriptionId, user.getPrenom(), user.getEmail());
                try {
                    sendReminderEmail(user.getEmail(), user.getPrenom(), subscriptionId, endDate);
                    sentReminderIds.add(subscriptionId); // Mark reminder as sent
                } catch (RuntimeException e) {
                    logger.error("Failed to send reminder email for subscription ID {}: {}", subscriptionId, e.getMessage());
                }
            } else {
                logger.debug("Subscription ID {} for user {} ({}) is not due for reminder. End date: {}",
                        subscriptionId, user.getPrenom(), user.getEmail(), endDate);
            }
        }
    }

    private void sendReminderEmail(String email, String prenom, Long subscriptionId, LocalDateTime endDate) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email);
            helper.setCc(financeEmail);
            helper.setSubject("🔔 Rappel : Votre abonnement arrive à expiration");
            helper.setFrom("damino.awadi@gmail.com");

            String htmlContent = """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        background-color: #f8f9fa;
                        margin: 0;
                        padding: 0;
                        color: #333333;
                        line-height: 1.6;
                    }
                    .container {
                        max-width: 600px;
                        margin: 30px auto;
                        background-color: #ffffff;
                        border-radius: 10px;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                        overflow: hidden;
                    }
                    .header {
                        background-color: #007BFF;
                        color: white;
                        text-align: center;
                        padding: 20px;
                        font-size: 20px;
                        font-weight: bold;
                    }
                    .content {
                        padding: 25px;
                    }
                    .content p {
                        margin-bottom: 15px;
                    }
                    .highlight {
                        color: #007BFF;
                        font-weight: bold;
                    }
                    .footer {
                        background-color: #f1f1f1;
                        text-align: center;
                        padding: 15px;
                        font-size: 13px;
                        color: #555555;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        🔔 Rappel d'expiration d'abonnement
                    </div>
                    <div class="content">
                        <p>Bonjour <strong>%s</strong>,</p>
                        <p>Nous vous rappelons que votre abonnement sur la plateforme <span class="highlight">VitalFeed</span> arrivera à expiration le <strong>%s</strong>.</p>
                        <p>⏳ Il vous reste moins de 7 jours pour le renouveler afin d’éviter toute interruption de service.</p>
                        <p>Pour renouveler votre abonnement, veuillez vous connecter à votre espace client dès maintenant.</p>
                        <p>Merci de votre confiance et de votre fidélité 💙</p>
                        <p>Cordialement,<br><strong>L’équipe VitalFeed</strong></p>
                    </div>
                    <div class="footer">
                        © 2025 VitalFeed – Tous droits réservés | Support : support@veterinaire.com
                    </div>
                </div>
            </body>
            </html>
        """.formatted(prenom, subscriptionId, endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            helper.setText(htmlContent, true);
            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email de rappel HTML", e);
        }
    }

}