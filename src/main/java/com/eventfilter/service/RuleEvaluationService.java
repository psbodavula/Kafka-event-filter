package com.eventfilter.service;

import com.eventfilter.engine.RuleEngine;
import com.eventfilter.model.FilterRule;
import com.eventfilter.repository.FilterRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final ColumnMappingService columnMappingService;

    public List<FilterRule> findMatchingRules(Map<String, Object> eventPayload, String topic) {
        List<FilterRule> applicableRules = getApplicableRules(topic);
        Map<String, String> columnMappings = columnMappingService.getMappingLookup();

        return applicableRules.stream()
                .filter(rule -> ruleEngine.evaluate(rule, eventPayload, topic, columnMappings))
                .toList();
    }

    public Optional<FilterRule> findFirstMatchingRule(Map<String, Object> eventPayload, String topic) {
        List<FilterRule> applicableRules = getApplicableRules(topic);
        Map<String, String> columnMappings = columnMappingService.getMappingLookup();

        return applicableRules.stream()
                .filter(rule -> ruleEngine.evaluate(rule, eventPayload, topic, columnMappings))
                .findFirst();
    }

    private List<FilterRule> getApplicableRules(String topic) {
        List<FilterRule> allRules = ruleRepository.findByEnabledTrueOrderByPriorityAsc();
        List<FilterRule> topicRules = ruleRepository.findByTopicsContainingAndEnabledTrueOrderByPriorityAsc(topic);

        List<FilterRule> globalRules = allRules.stream()
                .filter(r -> r.getTopics() == null || r.getTopics().isEmpty())
                .toList();

        return java.util.stream.Stream.concat(globalRules.stream(), topicRules.stream())
                .distinct()
                .sorted(java.util.Comparator.comparingInt(FilterRule::getPriority))
                .toList();
    }
}
