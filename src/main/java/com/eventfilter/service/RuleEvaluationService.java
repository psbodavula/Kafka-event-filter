package com.eventfilter.service;

import com.eventfilter.engine.RuleEngine;
import com.eventfilter.model.FilterRule;
import com.eventfilter.model.LogicalOperator;
import com.eventfilter.model.RuleType;
import com.eventfilter.repository.FilterRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEvaluationService {

    private final FilterRuleRepository ruleRepository;
    private final RuleEngine ruleEngine;

    /**
     * Find all matching rules for a given event payload on a given topic.
     */
    public List<FilterRule> findMatchingRules(Map<String, Object> eventPayload, String topic) {
        List<FilterRule> applicableRules = getApplicableRules(topic);

        return applicableRules.stream()
                .filter(rule -> evaluateRule(rule, eventPayload, topic))
                .toList();
    }

    /**
     * Find the first (highest priority) matching rule.
     */
    public Optional<FilterRule> findFirstMatchingRule(Map<String, Object> eventPayload, String topic) {
        List<FilterRule> applicableRules = getApplicableRules(topic);

        return applicableRules.stream()
                .filter(rule -> evaluateRule(rule, eventPayload, topic))
                .findFirst();
    }

    private boolean evaluateRule(FilterRule rule, Map<String, Object> eventPayload, String topic) {
        if (rule.getRuleType() == RuleType.COMPOSITE) {
            return evaluateCompositeRule(rule, eventPayload, topic);
        }
        return ruleEngine.evaluate(rule, eventPayload, topic);
    }

    private boolean evaluateCompositeRule(FilterRule rule, Map<String, Object> eventPayload, String topic) {
        if (rule.getChildRuleIds() == null || rule.getChildRuleIds().isEmpty()) {
            return false;
        }

        List<FilterRule> childRules = ruleRepository.findAllById(rule.getChildRuleIds());
        LogicalOperator operator = rule.getCompositeOperator() != null
                ? rule.getCompositeOperator()
                : LogicalOperator.AND;

        if (operator == LogicalOperator.AND) {
            return childRules.stream()
                    .allMatch(child -> evaluateRule(child, eventPayload, topic));
        } else {
            return childRules.stream()
                    .anyMatch(child -> evaluateRule(child, eventPayload, topic));
        }
    }

    private List<FilterRule> getApplicableRules(String topic) {
        List<FilterRule> allRules = ruleRepository.findByEnabledTrueOrderByPriorityAsc();
        List<FilterRule> topicRules = ruleRepository.findByTopicsContainingAndEnabledTrueOrderByPriorityAsc(topic);

        // Merge: rules with no topic restriction + rules specifically for this topic
        List<FilterRule> globalRules = allRules.stream()
                .filter(r -> r.getTopics() == null || r.getTopics().isEmpty())
                .toList();

        return java.util.stream.Stream.concat(globalRules.stream(), topicRules.stream())
                .distinct()
                .sorted(java.util.Comparator.comparingInt(FilterRule::getPriority))
                .toList();
    }
}
