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

    // ✅ Constructor injection (FIXES null issue)
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


    public void sendEmail(String to, String subject, String htmlContent) {

        String url = "https://api.brevo.com/v3/smtp/email";

        try {
            // ✅ Headers
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

            // ✅ Body
            Map<String, Object> body = new HashMap<>();
            body.put("sender", sender);
            body.put("to", toList);
            body.put("subject", subject);
            body.put("htmlContent", htmlContent);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            // ✅ Send request
            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, request, String.class);

            // ✅ Success log
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✅ Email sent successfully to: " + to);
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