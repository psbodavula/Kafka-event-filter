package com.eventfilter.converter.mapper.engine;

import com.eventfilter.converter.mapper.config.ConditionEntry;
import com.eventfilter.converter.mapper.config.MappingConfiguration;
import com.eventfilter.converter.mapper.config.MappingRuleDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic data-driven mapping engine that transforms a source payload (nested Map)
 * into target fields based on rules loaded from JSON configuration.
 *
 * <p>Rule types supported:
 * <ul>
 *   <li>{@code MAP_AS_IS} — direct value copy via xpath navigation</li>
 *   <li>{@code CONDITIONAL} — value-based lookup (if source matches condition, output mapped value)</li>
 *   <li>{@code HARDCODE} — fixed output value</li>
 *   <li>{@code EXPRESSION} — built-in expressions (e.g., CURRENT_TIMESTAMP)</li>
 *   <li>{@code PRESENCE_CHECK} — evaluate nested rules only if source field exists</li>
 * </ul>
 *
 * <p>Mapping configurations are loaded from classpath and cached for performance.
 */
@Slf4j
@Component
public class DataDrivenMappingEngine {

    private final ObjectMapper objectMapper;
    private final Map<String, MappingConfiguration> configCache = new ConcurrentHashMap<>();

    public DataDrivenMappingEngine(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        loadConfiguration("mapping/upo-to-gpi-mapping.json");
    }

    /**
     * Load a mapping configuration from classpath and cache it by mapping name.
     */
    public MappingConfiguration loadConfiguration(String classpathResource) {
        try {
            ClassPathResource resource = new ClassPathResource(classpathResource);
            try (InputStream is = resource.getInputStream()) {
                MappingConfiguration config = objectMapper.readValue(is, MappingConfiguration.class);
                configCache.put(config.getMappingName(), config);
                log.info("Loaded mapping configuration '{}' v{} with {} rules",
                        config.getMappingName(), config.getVersion(),
                        config.getRules() != null ? config.getRules().size() : 0);
                return config;
            }
        } catch (IOException e) {
            log.error("Failed to load mapping configuration from '{}': {}", classpathResource, e.getMessage());
            throw new IllegalStateException("Cannot load mapping config: " + classpathResource, e);
        }
    }

    /**
     * Apply a named mapping configuration to the given source payload.
     *
     * @param mappingName the mapping configuration name (e.g., "UPO_TO_GPI_STATUS_UPDATE")
     * @param source      the source data as a nested Map (from JSON payload)
     * @return mapping result containing output fields and any errors
     */
    public MappingResult map(String mappingName, Map<String, Object> source) {
        MappingConfiguration config = configCache.get(mappingName);
        if (config == null) {
            throw new IllegalArgumentException("No mapping configuration found for: " + mappingName);
        }
        return applyRules(config.getRules(), source);
    }

    /**
     * Apply mapping rules from a given configuration object directly.
     */
    public MappingResult map(MappingConfiguration config, Map<String, Object> source) {
        return applyRules(config.getRules(), source);
    }

    private MappingResult applyRules(List<MappingRuleDefinition> rules, Map<String, Object> source) {
        MappingResult result = new MappingResult();

        if (rules == null || rules.isEmpty()) {
            result.addWarning("No mapping rules defined");
            return result;
        }

        for (MappingRuleDefinition rule : rules) {
            try {
                applyRule(rule, source, result);
            } catch (Exception e) {
                result.addError("Error applying rule for '" + rule.getTargetField() + "': " + e.getMessage());
                log.error("Error applying mapping rule for field '{}': {}", rule.getTargetField(), e.getMessage(), e);
            }
        }

        return result;
    }

    private void applyRule(MappingRuleDefinition rule, Map<String, Object> source, MappingResult result) {
        String type = rule.getType();

        switch (type) {
            case "MAP_AS_IS" -> applyMapAsIs(rule, source, result);
            case "CONDITIONAL" -> applyConditional(rule, source, result);
            case "HARDCODE" -> applyHardcode(rule, result);
            case "EXPRESSION" -> applyExpression(rule, result);
            case "PRESENCE_CHECK" -> applyPresenceCheck(rule, source, result);
            default -> result.addWarning("Unknown rule type '" + type + "' for field '" + rule.getTargetField() + "'");
        }
    }

    private void applyMapAsIs(MappingRuleDefinition rule, Map<String, Object> source, MappingResult result) {
        Object value = resolveXpath(source, rule.getSourceXpath());

        if (value == null && rule.getFallbackSourceXpath() != null) {
            value = resolveXpath(source, rule.getFallbackSourceXpath());
        }

        if (value != null) {
            result.putField(rule.getTargetField(), value);
        } else if ("REQUIRED".equalsIgnoreCase(rule.getPresence())) {
            String error = rule.getErrorOnMissing() != null
                    ? rule.getErrorOnMissing()
                    : "Required field '" + rule.getTargetField() + "' is missing (source: " + rule.getSourceXpath() + ")";
            result.addError(error);
        }
    }

    private void applyConditional(MappingRuleDefinition rule, Map<String, Object> source, MappingResult result) {
        Object rawValue = resolveXpath(source, rule.getSourceXpath());

        if (rawValue == null) {
            if ("REQUIRED".equalsIgnoreCase(rule.getPresence())) {
                String error = rule.getErrorOnMissing() != null
                        ? rule.getErrorOnMissing()
                        : "Required source field missing for conditional mapping of '" + rule.getTargetField() + "'";
                result.addError(error);
            }
            return;
        }

        String sourceValue = String.valueOf(rawValue);
        List<ConditionEntry> conditions = rule.getConditions();

        if (conditions != null) {
            for (ConditionEntry condition : conditions) {
                if (condition.getMatchValues() != null && condition.getMatchValues().contains(sourceValue)) {
                    result.putField(rule.getTargetField(), condition.getOutputValue());
                    return;
                }
            }
        }

        if (rule.getErrorOnNoMatch() != null) {
            result.addError(rule.getErrorOnNoMatch());
        } else {
            result.addWarning("No condition matched for field '" + rule.getTargetField()
                    + "' with source value '" + sourceValue + "'");
        }
    }

    private void applyHardcode(MappingRuleDefinition rule, MappingResult result) {
        if (rule.getHardcodedValue() != null) {
            result.putField(rule.getTargetField(), rule.getHardcodedValue());
        }
    }

    private void applyExpression(MappingRuleDefinition rule, MappingResult result) {
        String expr = rule.getExpression();
        if ("CURRENT_TIMESTAMP".equals(expr)) {
            result.putField(rule.getTargetField(), Instant.now().toString());
        } else {
            result.addWarning("Unknown expression '" + expr + "' for field '" + rule.getTargetField() + "'");
        }
    }

    private void applyPresenceCheck(MappingRuleDefinition rule, Map<String, Object> source, MappingResult result) {
        Object value = resolveXpath(source, rule.getSourceXpath());

        if (value != null) {
            if (rule.getThenRules() != null) {
                for (MappingRuleDefinition thenRule : rule.getThenRules()) {
                    applyRule(thenRule, source, result);
                }
            }
        } else {
            if (rule.getFallbackSourceXpath() != null) {
                Object fallback = resolveXpath(source, rule.getFallbackSourceXpath());
                if (fallback != null) {
                    result.putField(rule.getTargetField(), fallback);
                    return;
                }
            }

            if ("REQUIRED".equalsIgnoreCase(rule.getPresence())) {
                String error = rule.getErrorOnMissing() != null
                        ? rule.getErrorOnMissing()
                        : "Required field '" + rule.getTargetField() + "' source not present";
                result.addError(error);
            }
        }
    }

    /**
     * Navigate a nested Map using a slash-separated xpath.
     * For example, "pmtInf/pmtId/uetr" traverses source["pmtInf"]["pmtId"]["uetr"].
     */
    @SuppressWarnings("unchecked")
    public Object resolveXpath(Map<String, Object> source, String xpath) {
        if (xpath == null || xpath.isBlank() || source == null) {
            return null;
        }

        String[] segments = xpath.split("/");
        Object current = source;

        for (String segment : segments) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(segment);
            } else {
                return null;
            }
            if (current == null) {
                return null;
            }
        }

        return current;
    }

    /**
     * Get a cached mapping configuration by name.
     */
    public MappingConfiguration getConfiguration(String mappingName) {
        return configCache.get(mappingName);
    }

    /**
     * Reload a mapping configuration (e.g., after external update).
     */
    public MappingConfiguration reloadConfiguration(String classpathResource) {
        return loadConfiguration(classpathResource);
    }
}
