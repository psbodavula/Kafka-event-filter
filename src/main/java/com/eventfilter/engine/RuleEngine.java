package com.eventfilter.engine;

import com.eventfilter.model.FilterRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class RuleEngine {

    /**
     * Evaluate a rule against an event payload.
     * A rule matches when ALL its column-value conditions equal the event's field values.
     */
    public boolean evaluate(FilterRule rule, Map<String, Object> eventPayload, String topic) {
        if (!isRuleApplicable(rule, topic)) {
            return false;
        }

        Map<String, String> conditions = rule.getConditions();

        if (conditions.isEmpty()) {
            return true;
        }

        for (Map.Entry<String, String> entry : conditions.entrySet()) {
            String column = entry.getKey();
            String expectedValue = entry.getValue();
            Object actualValue = resolveFieldValue(eventPayload, column);

            if (actualValue == null || !actualValue.toString().equals(expectedValue)) {
                log.debug("Rule '{}': column '{}' mismatch (expected='{}', actual='{}')",
                        rule.getName(), column, expectedValue, actualValue);
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
    private Object resolveFieldValue(Map<String, Object> payload, String fieldName) {
        if (!fieldName.contains(".")) {
            return payload.get(fieldName);
        }

        String[] parts = fieldName.split("\\.");
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
