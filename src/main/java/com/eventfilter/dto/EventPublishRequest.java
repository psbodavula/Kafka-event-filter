package com.eventfilter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventPublishRequest {

    @NotBlank(message = "Topic is required")
    private String topic;

    private String key;

    @NotNull(message = "Payload is required")
    private Map<String, Object> payload;
}
