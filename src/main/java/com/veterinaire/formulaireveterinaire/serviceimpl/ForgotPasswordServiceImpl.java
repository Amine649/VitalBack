package com.veterinaire.formulaireveterinaire.serviceimpl;

import com.veterinaire.formulaireveterinaire.Config.BrevoEmailService;
import com.veterinaire.formulaireveterinaire.DAO.UserRepository;
import com.veterinaire.formulaireveterinaire.entity.User;
import com.veterinaire.formulaireveterinaire.service.ForgotPasswordService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ForgotPasswordServiceImpl implements ForgotPasswordService {

    private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordServiceImpl.class);

    private final UserRepository userRepository;
    private final BrevoEmailService emailService;
    private final PasswordEncoder passwordEncoder;



    private static final int OTP_VALIDITY_MINUTES = 5;
    private static final int OTP_LENGTH = 6;


    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int number = 100000 + random.nextInt(900000); // 100000 → 999999
        return String.format("%06d", number);
    }

    private void clearOtp(User user) {
        user.setPasswordResetOtp(null);
        user.setPasswordResetOtpExpiry(null);
        user.setPasswordResetOtpUsed(true);
    }

    /**
     * Step 1: User requests OTP → generate 6-digit code → save in user → send email
     */
    @Override
    @Transactional
    public String requestOtp(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email requis");
        }

        Optional<User> userOpt = userRepository.findByEmail(email.trim().toLowerCase());
        if (userOpt.isEmpty()) {
            // On lance une exception si email n'existe pas
            throw new EntityNotFoundException("Cet email n'existe pas dans notre base de données.");
        }

        User user = userOpt.get();

        // Générer OTP sécurisé à 6 chiffres
        String otp = generateOtp();

        // Stocker OTP + expiry + used flag
        user.setPasswordResetOtp(otp);
        user.setPasswordResetOtpExpiry(LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES));
        user.setPasswordResetOtpUsed(false);

        userRepository.save(user);

        // Envoyer email
        sendOtpEmail(user, otp);

        logger.info("OTP {} sent to {}", otp, email);

        return "Un code de vérification a été envoyé à votre adresse email. Il est valide pendant 5 minutes.";
    }


    /**
     * Step 2: User submits OTP + new password → verify → update password
     */
    @Override
    @Transactional
    public String verifyOtpAndChangePassword(String email, String otp, String newPassword) {
        if (email == null || otp == null || otp.length() != OTP_LENGTH ||
                newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException(
                    "Données invalides : email, code OTP (6 chiffres), mot de passe (≥ 8 caractères) requis"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new EntityNotFoundException("Code ou email invalide"));

        // Vérification OTP
        if (user.getPasswordResetOtp() == null ||
                !user.getPasswordResetOtp().equals(otp) ||
                user.getPasswordResetOtpExpiry() == null ||
                user.getPasswordResetOtpExpiry().isBefore(now) ||
                user.isPasswordResetOtpUsed()) {

            clearOtp(user); // Optionnel : réinitialise OTP pour anti-brute-force
            userRepository.save(user);

            throw new SecurityException("Code OTP invalide, expiré ou déjà utilisé");
        }

        // Succès → changement de mot de passe
        user.setPassword(passwordEncoder.encode(newPassword));

        // Invalider OTP
        clearOtp(user);

        userRepository.save(user);

        logger.info("Password reset successful for {}", email);

        return "Votre mot de passe a été réinitialisé avec succès.";
    }




    private void sendOtpEmail(User user, String otp) {

        try {
            String nom = user.getNom() != null ? user.getNom() : "Cher utilisateur";
            String subject = "Code de vérification Compte – VITALFEED –";

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
                                <h2 style="color:#2c3e50;">Votre code de vérification</h2>
                                <p style="font-size:15px; line-height:1.6;">
                                    Bonjour Dr <strong>%s</strong>,<br><br>
                                    Voici le code à usage unique pour réinitialiser votre mot de passe :
                                </p>

                                <div style="margin:30px 0; text-align:center; font-size:36px; font-weight:bold; letter-spacing:12px; color:#00897B; background:#f0f8f5; padding:20px; border-radius:12px; border:1px solid #c3e8df;">
                                    %s
                                </div>

                                <p style="font-size:14px; color:#555; text-align:center; line-height:1.5;">
                                    Ce code expire dans <strong>5 minutes</strong>.<br>
                                    Ne le partagez jamais avec qui que ce soit.
                                </p>

                                <p style="font-size:13px; color:#777; margin-top:30px; text-align:center;">
                                    Si vous n'avez pas demandé ce code, ignorez cet email.
                                </p>

                                <div style="margin-top:40px;">
                                    <p style="margin-top:20px; font-weight:600; text-align:center;">Cordialement,</p>
                                    <p style="margin-top:5px; color:#00897B; font-weight:700; text-align:center;">L’équipe VITALFEED</p>
                                </div>
                            </td>
                        </tr>

                        <tr>
                            <td style="background-color:#f0f3f7; padding:15px 30px; text-align:center; font-size:12px; color:#777;">
                                Cet e-mail a été envoyé automatiquement – merci de ne pas y répondre.<br>
                                © %s VITALFEED – Tous droits réservés.
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(
                    nom,
                    otp,
                    String.valueOf(LocalDate.now().getYear())
            );

            emailService.sendEmail(user.getEmail(), subject, htmlContent, null);

            logger.info("OTP email sent to {}", user.getEmail());

        } catch (Exception e) {
            logger.error("Failed to send OTP email to {}: {}", user.getEmail(), e.getMessage());
        }
    }
}