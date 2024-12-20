package com.example.demo.service;


import com.example.demo.dto.NotificationResponse; // Assuming this is the response POJO we created
import com.example.demo.dto.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.transaction.annotation.Transactional;


@Service
public class NotificationService {

    @Value("${api.url}")
    private String apiUrl;

    @Value("${api.header.database}")
    private String databaseHeader;

    @Value("${api.header.applicationId}")
    private String applicationIdHeader;

    @Value("${api.header.authorization}")
    private String authorizationHeader;

    private final RestTemplate restTemplate;

    @Autowired
    public NotificationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendNotification(Request notificationRequest) {
        System.out.println("inside http request method");
        try {
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("database", databaseHeader);
            headers.set("applicationId", applicationIdHeader);
            headers.set("Authorization", authorizationHeader);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create the request entity
            HttpEntity<Request> entity = new HttpEntity<>(notificationRequest, headers);

            // Make the API call
            ResponseEntity<NotificationResponse> response = restTemplate.postForEntity(apiUrl, entity, NotificationResponse.class);

            // Check the response status and handle it
            if (response.getStatusCode().is2xxSuccessful()) {
                NotificationResponse responseBody = response.getBody();
                if (responseBody != null && "Success".equalsIgnoreCase(responseBody.getStatus())) {
                    System.out.println("Notification sent successfully: " + responseBody);
                } else {
                    System.out.println("Notification sent, but status indicates failure: " + responseBody);
                }
            } else {
                System.out.println("Failed to send notification: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("Error during API call: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
