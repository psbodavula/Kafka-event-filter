package com.eventfilter.swift.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Data
@Configuration
@ConfigurationProperties(prefix = "swift.api")
public class SwiftApiConfig {

    /**
     * SWIFT API base URL.
     * Sandbox: https://sandbox.swift.com
     * Production: https://api.swift.com
     */
    private String baseUrl = "https://sandbox.swift.com";

    /**
     * GPI Tracker API version path.
     */
    private String trackerPath = "/swift-apitracker/v5";

    /**
     * SWIFT API key (OAuth2 client credentials or API key).
     */
    private String apiKey;

    /**
     * OAuth2 client ID for SWIFT API authentication.
     */
    private String clientId;

    /**
     * OAuth2 client secret for SWIFT API authentication.
     */
    private String clientSecret;

    /**
     * BIC of the institution making the API call.
     */
    private String institutionBic;

    /**
     * Connection timeout in milliseconds.
     */
    private int connectTimeout = 10000;

    /**
     * Read timeout in milliseconds.
     */
    private int readTimeout = 30000;

    /**
     * Enable/disable actual API calls (useful for testing).
     */
    private boolean enabled = false;

    @Bean("swiftRestTemplate")
    public RestTemplate swiftRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        RestTemplate restTemplate = new RestTemplate(factory);
        return restTemplate;
    }

    public String getStatusUpdateUrl(String uetr) {
        return baseUrl + trackerPath + "/payments/" + uetr + "/status";
    }
}
