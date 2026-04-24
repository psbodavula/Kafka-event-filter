package com.eventfilter.service;

import com.eventfilter.model.FilterRule;
import com.eventfilter.model.FilteredEvent;
import com.eventfilter.model.RuleAction;
import com.eventfilter.repository.FilteredEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventProcessingService {

    private final RuleEvaluationService ruleEvaluationService;
    private final FilteredEventRepository filteredEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Process an incoming event from Kafka.
     * Evaluates all matching rules and executes the appropriate actions.
     */
    public void processEvent(String topic, Map<String, Object> eventPayload, Map<String, String> headers) {
        List<FilterRule> matchingRules = ruleEvaluationService.findMatchingRules(eventPayload, topic);

        if (matchingRules.isEmpty()) {
            log.debug("No matching rules for event on topic '{}': {}", topic, eventPayload);
            return;
        }

        for (FilterRule rule : matchingRules) {
            executeAction(rule, topic, eventPayload, headers);
        }
    }

    private void executeAction(FilterRule rule, String sourceTopic,
                               Map<String, Object> eventPayload, Map<String, String> headers) {
        log.info("Rule '{}' matched on topic '{}', action: {}", rule.getName(), sourceTopic, rule.getAction());

        switch (rule.getAction()) {
            case FORWARD -> forwardEvent(rule, sourceTopic, eventPayload, headers);
            case ROUTE -> routeEvent(rule, sourceTopic, eventPayload, headers);
            case DROP -> logDroppedEvent(rule, sourceTopic, eventPayload, headers);
        }
    }

    private void forwardEvent(FilterRule rule, String sourceTopic,
                              Map<String, Object> eventPayload, Map<String, String> headers) {
        String targetTopic = rule.getTargetTopic();
        if (targetTopic == null || targetTopic.isBlank()) {
            targetTopic = sourceTopic + ".filtered";
        }

        kafkaTemplate.send(targetTopic, eventPayload);
        saveFilteredEvent(rule, sourceTopic, targetTopic, eventPayload, headers);
        log.info("Forwarded event to topic '{}'", targetTopic);
    }

    private void routeEvent(FilterRule rule, String sourceTopic,
                            Map<String, Object> eventPayload, Map<String, String> headers) {
        String targetTopic = rule.getTargetTopic();
        if (targetTopic == null || targetTopic.isBlank()) {
            log.warn("ROUTE action for rule '{}' has no target topic configured", rule.getName());
            return;
        }

        kafkaTemplate.send(targetTopic, eventPayload);
        saveFilteredEvent(rule, sourceTopic, targetTopic, eventPayload, headers);
        log.info("Routed event to topic '{}'", targetTopic);
    }

    private void logDroppedEvent(FilterRule rule, String sourceTopic,
                                 Map<String, Object> eventPayload, Map<String, String> headers) {
        saveFilteredEvent(rule, sourceTopic, null, eventPayload, headers);
        log.info("Dropped event from topic '{}' (rule: '{}')", sourceTopic, rule.getName());
    }

    private void saveFilteredEvent(FilterRule rule, String sourceTopic, String targetTopic,
                                   Map<String, Object> eventPayload, Map<String, String> headers) {
        FilteredEvent event = FilteredEvent.builder()
                .sourceTopic(sourceTopic)
                .matchedRuleId(rule.getId())
                .matchedRuleName(rule.getName())
                .actionTaken(rule.getAction())
                .targetTopic(targetTopic)
                .eventPayload(eventPayload)
                .headers(headers)
                .build();
        filteredEventRepository.save(event);
    }
}
