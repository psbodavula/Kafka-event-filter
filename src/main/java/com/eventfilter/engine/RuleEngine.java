package com.eventfilter.engine;

import com.eventfilter.model.ConditionOperator;
import com.eventfilter.model.FieldCondition;
import com.eventfilter.model.FilterRule;
import com.eventfilter.model.LogicalOperator;
import com.eventfilter.model.RuleType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class RuleEngine {

    private final ExpressionParser spelParser = new SpelExpressionParser();

    /**
     * Evaluate a single rule against an event payload.
     */
    public boolean evaluate(FilterRule rule, Map<String, Object> eventPayload, String topic) {
        if (!isRuleApplicable(rule, topic)) {
            return false;
        }

        return switch (rule.getRuleType()) {
            case SPEL -> evaluateSpel(rule, eventPayload);
            case FIELD_MATCH -> evaluateFieldMatch(rule, eventPayload);
            case COMPOSITE -> evaluateComposite(rule, eventPayload, topic);
        };
    }

    private boolean isRuleApplicable(FilterRule rule, String topic) {
        if (rule.getTopics() == null || rule.getTopics().isEmpty()) {
            return true;
        }
        return rule.getTopics().contains(topic);
    }

    private boolean evaluateSpel(FilterRule rule, Map<String, Object> eventPayload) {
        try {
            EvaluationContext context = new StandardEvaluationContext();
            ((StandardEvaluationContext) context).setVariable("event", eventPayload);

            for (Map.Entry<String, Object> entry : eventPayload.entrySet()) {
                ((StandardEvaluationContext) context).setVariable(entry.getKey(), entry.getValue());
            }

            Expression expression = spelParser.parseExpression(rule.getSpelExpression());
            Boolean result = expression.getValue(context, Boolean.class);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("SpEL evaluation failed for rule '{}': {}", rule.getName(), e.getMessage());
            return false;
        }
    }

    private boolean evaluateFieldMatch(FilterRule rule, Map<String, Object> eventPayload) {
        if (rule.getFieldConditions() == null || rule.getFieldConditions().isEmpty()) {
            return true;
        }

        for (Map.Entry<String, FieldCondition> entry : rule.getFieldConditions().entrySet()) {
            String fieldName = entry.getKey();
            FieldCondition condition = entry.getValue();
            Object fieldValue = resolveFieldValue(eventPayload, fieldName);

            if (!evaluateCondition(fieldValue, condition)) {
                return false;
            }
        }
        return true;
    }

    private boolean evaluateComposite(FilterRule rule, Map<String, Object> eventPayload, String topic) {
        log.warn("Composite rule '{}' references child rules, but inline evaluation is not supported. "
                + "Use RuleEvaluationService for composite rules.", rule.getName());
        return false;
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

    @SuppressWarnings("unchecked")
    private boolean evaluateCondition(Object fieldValue, FieldCondition condition) {
        ConditionOperator operator = condition.getOperator();
        Object expectedValue = condition.getValue();

        return switch (operator) {
            case EXISTS -> (fieldValue != null) == Boolean.parseBoolean(String.valueOf(expectedValue));
            case EQUALS -> fieldValue != null && fieldValue.toString().equals(String.valueOf(expectedValue));
            case NOT_EQUALS -> fieldValue == null || !fieldValue.toString().equals(String.valueOf(expectedValue));
            case CONTAINS -> fieldValue != null && fieldValue.toString().contains(String.valueOf(expectedValue));
            case REGEX -> fieldValue != null && Pattern.matches(String.valueOf(expectedValue), fieldValue.toString());
            case GREATER_THAN -> compareNumeric(fieldValue, expectedValue) > 0;
            case LESS_THAN -> compareNumeric(fieldValue, expectedValue) < 0;
            case IN -> {
                if (expectedValue instanceof Collection) {
                    yield fieldValue != null && ((Collection<Object>) expectedValue)
                            .stream()
                            .anyMatch(v -> v.toString().equals(fieldValue.toString()));
                }
                yield false;
            }
            case NOT_IN -> {
                if (expectedValue instanceof Collection) {
                    yield fieldValue == null || ((Collection<Object>) expectedValue)
                            .stream()
                            .noneMatch(v -> v.toString().equals(fieldValue.toString()));
                }
                yield true;
            }
        };
    }

    private int compareNumeric(Object fieldValue, Object expectedValue) {
        try {
            double fieldNum = Double.parseDouble(String.valueOf(fieldValue));
            double expectedNum = Double.parseDouble(String.valueOf(expectedValue));
            return Double.compare(fieldNum, expectedNum);
        } catch (NumberFormatException e) {
            log.warn("Cannot compare non-numeric values: {} vs {}", fieldValue, expectedValue);
            return 0;
        }
    }
}
