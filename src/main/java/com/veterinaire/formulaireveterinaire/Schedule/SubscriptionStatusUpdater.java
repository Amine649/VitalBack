package com.veterinaire.formulaireveterinaire.Schedule;


import com.veterinaire.formulaireveterinaire.Enums.SubscriptionStatus;
import com.veterinaire.formulaireveterinaire.DAO.SubscriptionRepository;
import com.veterinaire.formulaireveterinaire.DAO.UserRepository;
import com.veterinaire.formulaireveterinaire.entity.Subscription;
import com.veterinaire.formulaireveterinaire.entity.User;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class SubscriptionStatusUpdater {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionStatusUpdater.class);

    public SubscriptionStatusUpdater(SubscriptionRepository subscriptionRepository, UserRepository userRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
    }

    @Scheduled(cron = "0 * * * * *") // Run every minute (for testing)
    // @Scheduled(cron = "0 0 0 * * *") // Run daily at midnight
    public void updateExpiredSubscriptions() {
        logger.info("Checking for expired subscriptions at {}", LocalDateTime.now());
        List<Subscription> subscriptions = subscriptionRepository.findAll();

        if (subscriptions == null || subscriptions.isEmpty()) {
            logger.info("No subscriptions found to check.");
            return;
        }

        for (Subscription subscription : subscriptions) {
            if (subscription == null || subscription.getUser() == null) {
                logger.warn("Invalid subscription or user data encountered, skipping.");
                continue;
            }

            User user = subscription.getUser();
            LocalDateTime endDate = subscription.getEndDate();

            if (endDate != null && endDate.isBefore(LocalDateTime.now()) && user.getStatus() != SubscriptionStatus.EXPIRED) {
                user.setStatus(SubscriptionStatus.EXPIRED);
                userRepository.save(user);
                logger.info("Subscription with ID {} has expired. User {}'s status updated to EXPIRED. End date was: {}",
                        subscription.getId(), user.getPrenom(), endDate);
            } else if (endDate != null && !endDate.isBefore(LocalDateTime.now())) {
                logger.debug("Subscription with ID {} is still active. End date: {}", subscription.getId(), endDate);
            }
        }
    }
}
