package com.eventfilter.swift.service;

import com.eventfilter.converter.mapper.DataDrivenGpiMapper;
import com.eventfilter.converter.mapper.UpoToGpiRequestMapper;
import com.eventfilter.model.upo.UniversalPaymentObject;
import com.eventfilter.swift.config.SwiftApiConfig;
import com.eventfilter.swift.model.GpiStatusUpdateRequest;
import com.eventfilter.swift.model.GpiStatusUpdateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
public class SwiftGpiTrackerService {

    private final SwiftApiConfig swiftApiConfig;
    private final RestTemplate swiftRestTemplate;
    private final UpoToGpiRequestMapper mapper;
    private final DataDrivenGpiMapper dataDrivenMapper;

    public SwiftGpiTrackerService(SwiftApiConfig swiftApiConfig,
                                   @Qualifier("swiftRestTemplate") RestTemplate swiftRestTemplate,
                                   UpoToGpiRequestMapper mapper,
                                   DataDrivenGpiMapper dataDrivenMapper) {
        this.swiftApiConfig = swiftApiConfig;
        this.swiftRestTemplate = swiftRestTemplate;
        this.mapper = mapper;
        this.dataDrivenMapper = dataDrivenMapper;
    }

    /**
     * Update payment status using the legacy UPO-based mapper (MapStruct).
     */
    public GpiStatusUpdateResponse updatePaymentStatus(UniversalPaymentObject upo) {
        if (upo.getUetr() == null || upo.getUetr().isBlank()) {
            log.error("Cannot update GPI tracker: UETR is missing from UPO");
            return GpiStatusUpdateResponse.builder()
                    .success(false)
                    .errorMessage("UETR is required for GPI tracker update")
                    .build();
        }

        if (!swiftApiConfig.isEnabled()) {
            log.info("SWIFT API is disabled. Simulating GPI tracker update for UETR: {}", upo.getUetr());
            return simulateStatusUpdate(upo);
        }

        return executeStatusUpdate(upo);
    }

    /**
     * Update payment status using the data-driven mapper.
     * Maps raw Kafka JSON payload directly to the GPI request using configurable rules.
     *
     * @param rawPayload the raw Kafka JSON message as a nested Map
     * @return the GPI status update response
     */
    public GpiStatusUpdateResponse updatePaymentStatusDataDriven(Map<String, Object> rawPayload) {
        DataDrivenGpiMapper.GpiMappingResult mappingResult = dataDrivenMapper.toGpiRequest(rawPayload);

        if (mappingResult.hasErrors()) {
            log.warn("Data-driven mapping produced errors: {}", mappingResult.getErrors());
            return GpiStatusUpdateResponse.builder()
                    .success(false)
                    .errorMessage("Mapping errors: " + String.join("; ", mappingResult.getErrors()))
                    .build();
        }

        GpiStatusUpdateRequest request = mappingResult.getRequest();
        if (request == null) {
            return GpiStatusUpdateResponse.builder()
                    .success(false)
                    .errorMessage("Data-driven mapping produced no output")
                    .build();
        }

        String uetr = dataDrivenMapper.extractUetr(rawPayload);
        if (uetr == null || uetr.isBlank()) {
            log.error("Cannot update GPI tracker: UETR not found in raw payload");
            return GpiStatusUpdateResponse.builder()
                    .success(false)
                    .errorMessage("UETR is required for GPI tracker update")
                    .build();
        }

        if (swiftApiConfig.getInstitutionBic() != null) {
            request.setTrackerInformingParty(swiftApiConfig.getInstitutionBic());
        }

        if (!swiftApiConfig.isEnabled()) {
            log.info("SWIFT API disabled. Simulating data-driven GPI update for UETR: {}", uetr);
            return GpiStatusUpdateResponse.builder()
                    .success(true)
                    .transactionStatus(request.getTransactionStatus())
                    .confirmationNumber("SIM-DD-" + System.currentTimeMillis())
                    .httpStatusCode(200)
                    .build();
        }

        return executeApiCall(uetr, request);
    }

    private GpiStatusUpdateResponse executeStatusUpdate(UniversalPaymentObject upo) {
        GpiStatusUpdateRequest request = mapper.toGpiRequest(upo);
        request.setTrackerInformingParty(swiftApiConfig.getInstitutionBic());
        return executeApiCall(upo.getUetr(), request);
    }

    private GpiStatusUpdateResponse executeApiCall(String uetr, GpiStatusUpdateRequest request) {
        String url = swiftApiConfig.getStatusUpdateUrl(uetr);

        try {
            HttpHeaders headers = buildHeaders();
            HttpEntity<GpiStatusUpdateRequest> entity = new HttpEntity<>(request, headers);

            log.info("Calling SWIFT GPI Tracker API: PUT {} for UETR: {}", url, uetr);

            ResponseEntity<GpiStatusUpdateResponse> response = swiftRestTemplate.exchange(
                    url, HttpMethod.PUT, entity, GpiStatusUpdateResponse.class);

            GpiStatusUpdateResponse body = response.getBody();
            if (body != null) {
                body.setSuccess(response.getStatusCode().is2xxSuccessful());
                body.setHttpStatusCode(response.getStatusCode().value());
            }

            log.info("GPI Tracker update successful for UETR: {}, status: {}",
                    uetr, response.getStatusCode());
            return body;

        } catch (HttpClientErrorException e) {
            log.error("Client error calling SWIFT GPI API for UETR {}: {} - {}",
                    uetr, e.getStatusCode(), e.getResponseBodyAsString());
            return GpiStatusUpdateResponse.builder()
                    .success(false)
                    .httpStatusCode(e.getStatusCode().value())
                    .errorMessage(e.getResponseBodyAsString())
                    .build();

        } catch (HttpServerErrorException e) {
            log.error("Server error calling SWIFT GPI API for UETR {}: {} - {}",
                    uetr, e.getStatusCode(), e.getResponseBodyAsString());
            return GpiStatusUpdateResponse.builder()
                    .success(false)
                    .httpStatusCode(e.getStatusCode().value())
                    .errorMessage("SWIFT API server error: " + e.getResponseBodyAsString())
                    .build();

        } catch (Exception e) {
            log.error("Unexpected error calling SWIFT GPI API for UETR {}: {}",
                    uetr, e.getMessage(), e);
            return GpiStatusUpdateResponse.builder()
                    .success(false)
                    .errorMessage("Unexpected error: " + e.getMessage())
                    .build();
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);

        if (swiftApiConfig.getApiKey() != null && !swiftApiConfig.getApiKey().isBlank()) {
            headers.set("Authorization", "Bearer " + swiftApiConfig.getApiKey());
        }

        if (swiftApiConfig.getInstitutionBic() != null) {
            headers.set("X-BIC", swiftApiConfig.getInstitutionBic());
        }

        return headers;
    }

    private GpiStatusUpdateResponse simulateStatusUpdate(UniversalPaymentObject upo) {
        log.info("[SIMULATION] GPI Tracker status update for UETR: {}", upo.getUetr());
        log.info("[SIMULATION] From: {} -> To: {}", upo.getDebtorAgentBic(), upo.getCreditorAgentBic());
        log.info("[SIMULATION] Status: {}, Amount: {} {}",
                upo.getTransactionStatus(),
                upo.getInstructedAmount(),
                upo.getInstructedCurrency());

        return GpiStatusUpdateResponse.builder()
                .success(true)
                .transactionStatus(upo.getTransactionStatus() != null ? upo.getTransactionStatus() : "ACSP")
                .confirmationNumber("SIM-" + System.currentTimeMillis())
                .httpStatusCode(200)
                .build();
    }
}
