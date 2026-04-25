package com.eventfilter.converter;

import com.eventfilter.model.upo.UniversalPaymentObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class JsonToUpoConverter {

    private final ObjectMapper objectMapper;

    public JsonToUpoConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Convert a raw JSON payload (Map) to a UniversalPaymentObject.
     * Jackson handles all field mapping via @JsonAlias annotations on the UPO model.
     */
    public UniversalPaymentObject convert(Map<String, Object> payload) {
        log.debug("Converting JSON payload to UPO: {}", payload);
        return objectMapper.convertValue(payload, UniversalPaymentObject.class);
    }
}
