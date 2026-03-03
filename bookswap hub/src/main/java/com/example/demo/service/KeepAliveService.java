package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service to keep the Render deployment awake by pinging itself periodically.
 * Render's free tier spins down services after 15 minutes of inactivity.
 * This service pings the app every 14 minutes to prevent that.
 */
@Service
public class KeepAliveService {

    private static final Logger logger = LoggerFactory.getLogger(KeepAliveService.class);

    @Value("${app.base-url:#{null}}")
    private String baseUrl;

    @Value("${server.port:8080}")
    private String port;

    private final RestTemplate restTemplate;

    public KeepAliveService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Ping the application every 14 minutes (840000 ms) to keep it alive.
     * Uses the /login endpoint as it's configured as the health check path.
     */
    @Scheduled(fixedRate = 840000, initialDelay = 60000)
    public void keepAlive() {
        String url = getHealthCheckUrl();
        if (url == null) {
            logger.debug("Keep-alive skipped: No base URL configured (local development)");
            return;
        }

        try {
            restTemplate.getForObject(url, String.class);
            logger.info("Keep-alive ping successful to: {}", url);
        } catch (Exception e) {
            logger.warn("Keep-alive ping failed: {}", e.getMessage());
        }
    }

    private String getHealthCheckUrl() {
        if (baseUrl != null && !baseUrl.isBlank()) {
            return baseUrl + "/login";
        }
        // Return null for local development - no need to ping localhost
        return null;
    }
}
