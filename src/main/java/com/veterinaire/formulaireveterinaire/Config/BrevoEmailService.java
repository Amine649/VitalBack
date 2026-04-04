package com.veterinaire.formulaireveterinaire.Config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
public class BrevoEmailService {

    private final String apiKey;
    private final String senderEmail;
    private final String senderName;
    private final RestTemplate restTemplate;

    public BrevoEmailService(
            @Value("${brevo.api.key}") String apiKey,
            @Value("${brevo.sender.email}") String senderEmail,
            @Value("${brevo.sender.name}") String senderName
    ) {
        this.apiKey = apiKey;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.restTemplate = new RestTemplate();
    }

    public void sendEmail(String to, String subject, String htmlContent, String cc) {

        String url = "https://api.brevo.com/v3/smtp/email";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);

            // ✅ Sender
            Map<String, String> sender = new HashMap<>();
            sender.put("email", senderEmail);
            sender.put("name", senderName);

            // ✅ Recipient
            Map<String, String> toMap = new HashMap<>();
            toMap.put("email", to);
            List<Map<String, String>> toList = new ArrayList<>();
            toList.add(toMap);

            // ✅ BCC → sender gets a copy (appears in Sent / Inbox)
            Map<String, String> bccMap = new HashMap<>();
            bccMap.put("email", senderEmail);
            bccMap.put("name", senderName);
            List<Map<String, String>> bccList = new ArrayList<>();
            bccList.add(bccMap);

            // ✅ Body
            Map<String, Object> body = new HashMap<>();
            body.put("sender", sender);
            body.put("to", toList);
            body.put("subject", subject);
            body.put("htmlContent", htmlContent);
            body.put("bcc", bccList); // 👈 Always BCC the sender

            // ✅ CC (optional, passed from caller)
            if (cc != null && !cc.isBlank()) {
                Map<String, String> ccMap = new HashMap<>();
                ccMap.put("email", cc);
                List<Map<String, String>> ccList = new ArrayList<>();
                ccList.add(ccMap);
                body.put("cc", ccList); // 👈 CC recipient also gets it
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✅ Email sent to: " + to);
                System.out.println("✅ BCC copy sent to sender: " + senderEmail);
                if (cc != null && !cc.isBlank()) {
                    System.out.println("✅ CC copy sent to: " + cc);
                }
            } else {
                throw new RuntimeException("❌ Brevo error: " + response.getBody());
            }

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("❌ Brevo HTTP Error: " + e.getStatusCode());
            System.err.println("❌ Response body: " + e.getResponseBodyAsString());
            throw new RuntimeException("❌ Failed to send email via Brevo: " + e.getMessage(), e);
        }
    }
}