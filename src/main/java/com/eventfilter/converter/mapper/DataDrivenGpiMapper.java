package com.eventfilter.converter.mapper;

import com.eventfilter.converter.mapper.engine.DataDrivenMappingEngine;
import com.eventfilter.converter.mapper.engine.MappingResult;
import com.eventfilter.swift.model.GpiStatusUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * GPI-specific wrapper around {@link DataDrivenMappingEngine}.
 * Applies the "UPO_TO_GPI_STATUS_UPDATE" mapping rules to a raw payment payload
 * and produces a {@link GpiStatusUpdateRequest}.
 *
 * <p>This replaces the hardcoded MapStruct mapper with a fully configuration-driven approach.
 * Mapping rules are defined in {@code mapping/upo-to-gpi-mapping.json}.
 */
@Slf4j
@Component
public class DataDrivenGpiMapper {

    private static final String MAPPING_NAME = "UPO_TO_GPI_STATUS_UPDATE";

    private final DataDrivenMappingEngine engine;
    private final ObjectMapper objectMapper;

    public DataDrivenGpiMapper(DataDrivenMappingEngine engine, ObjectMapper objectMapper) {
        this.engine = engine;
        this.objectMapper = objectMapper;
    }

    /**
     * Map a raw payment payload (nested JSON Map from Kafka) directly to a GPI status update request.
     *
     * @param sourcePayload the raw Kafka JSON payload as a nested Map
     * @return the mapping result containing the GPI request and any errors
     */
    public GpiMappingResult toGpiRequest(Map<String, Object> sourcePayload) {
        MappingResult engineResult = engine.map(MAPPING_NAME, sourcePayload);

        GpiStatusUpdateRequest request = null;
        if (!engineResult.getOutputFields().isEmpty()) {
            request = objectMapper.convertValue(engineResult.getOutputFields(), GpiStatusUpdateRequest.class);
        }

        GpiMappingResult gpiResult = new GpiMappingResult();
        gpiResult.setRequest(request);
        gpiResult.setErrors(engineResult.getErrors());
        gpiResult.setWarnings(engineResult.getWarnings());

        if (engineResult.hasErrors()) {
            log.warn("Data-driven GPI mapping completed with {} error(s): {}",
                    engineResult.getErrors().size(), engineResult.getErrors());
        } else {
            log.debug("Data-driven GPI mapping completed successfully for payload");
        }

        return gpiResult;
    }

    /**
     * Extract the UETR from a raw payload using xpath navigation.
     * Used to determine whether GPI tracking should be invoked.
     */
    public String extractUetr(Map<String, Object> sourcePayload) {
        Object uetr = engine.resolveXpath(sourcePayload, "pmtInf/pmtId/uetr");
        if (uetr == null) {
            uetr = sourcePayload.get("uetr");
        }
        return uetr != null ? String.valueOf(uetr) : null;
    }

    @lombok.Data
    public static class GpiMappingResult {
        private GpiStatusUpdateRequest request;
        private java.util.List<String> errors = new java.util.ArrayList<>();
        private java.util.List<String> warnings = new java.util.ArrayList<>();

        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }
}
