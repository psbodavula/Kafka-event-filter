package com.eventfilter.converter.mapper.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of applying mapping rules via {@link DataDrivenMappingEngine}.
 * Contains the mapped output fields and any errors/warnings collected during evaluation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MappingResult {

    @Builder.Default
    private Map<String, Object> outputFields = new LinkedHashMap<>();

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void addError(String error) {
        errors.add(error);
    }

    public void addWarning(String warning) {
        warnings.add(warning);
    }

    public void putField(String key, Object value) {
        outputFields.put(key, value);
    }
}
