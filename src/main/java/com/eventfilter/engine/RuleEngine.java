package com.eventfilter.engine;

import com.eventfilter.model.FilterRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class RuleEngine {

    /**
     * Evaluate a rule against an event payload using column mappings from the database.
     * A rule matches when ALL its conditions equal the event's nested field values.
     *
     * @param rule           the filter rule
     * @param eventPayload   the Kafka event payload
     * @param topic          the Kafka topic
     * @param columnMappings map of columnName → payloadPath from column_mappings collection
     */
    public boolean evaluate(FilterRule rule, Map<String, Object> eventPayload,
                            String topic, Map<String, String> columnMappings) {
        if (!isRuleApplicable(rule, topic)) {
            return false;
        }

        Map<String, String> conditions = rule.getConditions(columnMappings);

        if (conditions.isEmpty()) {
            return true;
        }

        for (Map.Entry<String, String> entry : conditions.entrySet()) {
            String payloadPath = entry.getKey();
            String expectedValue = entry.getValue();
            Object actualValue = resolveFieldValue(eventPayload, payloadPath);

            if (actualValue == null || !actualValue.toString().equals(expectedValue)) {
                log.debug("Rule '{}': path '{}' mismatch (expected='{}', actual='{}')",
                        rule.getName(), payloadPath, expectedValue, actualValue);
                return false;
            }
        }

        log.debug("Rule '{}': all {} conditions matched", rule.getName(), conditions.size());
        return true;
    }

    private boolean isRuleApplicable(FilterRule rule, String topic) {
        if (rule.getTopics() == null || rule.getTopics().isEmpty()) {
            return true;
        }
        return rule.getTopics().contains(topic);
    }

    @SuppressWarnings("unchecked")
    private Object resolveFieldValue(Map<String, Object> payload, String path) {
        if (!path.contains("/") && !path.contains(".")) {
            return payload.get(path);
        }

        String[] parts = path.contains("/") ? path.split("/") : path.split("\\.");
        Object current = payload;
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return null;
            }
        }
        return current;
    }
}
