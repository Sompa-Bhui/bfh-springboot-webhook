package com.example.bfh.service;

import com.example.bfh.config.AppProperties;
import com.example.bfh.dto.GenerateWebhookRequest;
import com.example.bfh.dto.GenerateWebhookResponse;
import com.example.bfh.dto.SubmitSolutionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class WebhookService {
    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final RestTemplate restTemplate;
    private final AppProperties props;

    public WebhookService(RestTemplate restTemplate, AppProperties props) {
        this.restTemplate = restTemplate;
        this.props = props;
    }

    public void executeFlow() {
        // 1) Generate webhook
        String generateUrl = props.getBaseUrl() + "/generateWebhook/JAVA";
        GenerateWebhookRequest req = new GenerateWebhookRequest(props.getName(), props.getRegNo(), props.getEmail());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<GenerateWebhookRequest> entity = new HttpEntity<>(req, headers);

        log.info("Calling generate webhook API: {}", generateUrl);
        GenerateWebhookResponse resp;
        try {
            ResponseEntity<GenerateWebhookResponse> response = restTemplate.postForEntity(generateUrl, entity, GenerateWebhookResponse.class);
            resp = response.getBody();
        } catch (RestClientException ex) {
            log.error("Failed to call generateWebhook: {}", ex.getMessage(), ex);
            return;
        }

        if (resp == null || resp.getWebhook() == null || resp.getAccessToken() == null) {
            log.error("Invalid response from generateWebhook: {}", resp);
            return;
        }
        log.info("Received webhook URL: {}", resp.getWebhook());
        log.info("Received accessToken (JWT): {}...", resp.getAccessToken().substring(0, Math.min(12, resp.getAccessToken().length())));

        // 2) Decide which question set based on regNo last two digits
        String regNo = props.getRegNo();
        int lastTwo = 0;
        try {
            String digits = regNo.replaceAll("D", "");
            if (digits.length() >= 2) {
                lastTwo = Integer.parseInt(digits.substring(digits.length() - 2));
            } else if (digits.length() == 1) {
                lastTwo = Integer.parseInt(digits);
            }
        } catch (NumberFormatException e) {
            log.warn("Could not parse last two digits from regNo '{}'", regNo);
        }
        boolean isOdd = (lastTwo % 2) == 1;
        if (isOdd) {
            log.info("regNo last two digits are ODD -> Use Question 1 (Odd).");
        } else {
            log.info("regNo last two digits are EVEN -> Use Question 2 (Even).");
        }

        // 3) Submit solution (final SQL query) to the returned webhook URL with JWT in Authorization header
        SubmitSolutionRequest submit = new SubmitSolutionRequest(props.getFinalQuery());
        HttpHeaders submitHeaders = new HttpHeaders();
        submitHeaders.setContentType(MediaType.APPLICATION_JSON);
        // IMPORTANT: The spec says header must be exactly 'Authorization: <accessToken>' (no Bearer)
        submitHeaders.set("Authorization", resp.getAccessToken());
        HttpEntity<SubmitSolutionRequest> submitEntity = new HttpEntity<>(submit, submitHeaders);

        String webhookUrl = resp.getWebhook();
        log.info("Submitting finalQuery to webhook: {}", webhookUrl);
        try {
            ResponseEntity<String> submitResponse = restTemplate.postForEntity(webhookUrl, submitEntity, String.class);
            log.info("Submission status: {} {}", submitResponse.getStatusCode().value(), submitResponse.getStatusCode());
            log.info("Submission response body: {}", submitResponse.getBody());
        } catch (RestClientException ex) {
            log.error("Failed to submit finalQuery: {}", ex.getMessage(), ex);
        }
    }
}
