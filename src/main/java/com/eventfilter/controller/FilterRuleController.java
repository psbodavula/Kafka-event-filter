package com.eventfilter.controller;

import com.eventfilter.dto.FilterRuleRequest;
import com.eventfilter.dto.RuleTestRequest;
import com.eventfilter.dto.RuleTestResponse;
import com.eventfilter.model.FilterRule;
import com.eventfilter.service.FilterRuleService;
import com.eventfilter.service.RuleEvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class FilterRuleController {

    private final FilterRuleService filterRuleService;
    private final RuleEvaluationService ruleEvaluationService;

    @PostMapping
    public ResponseEntity<FilterRule> createRule(@Valid @RequestBody FilterRuleRequest request) {
        FilterRule rule = mapToEntity(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(filterRuleService.createRule(rule));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FilterRule> updateRule(@PathVariable String id,
                                                  @Valid @RequestBody FilterRuleRequest request) {
        FilterRule rule = mapToEntity(request);
        return ResponseEntity.ok(filterRuleService.updateRule(id, rule));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable String id) {
        filterRuleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<FilterRule> getRule(@PathVariable String id) {
        return filterRuleService.getRule(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<FilterRule>> getAllRules() {
        return ResponseEntity.ok(filterRuleService.getAllRules());
    }

    @GetMapping("/enabled")
    public ResponseEntity<List<FilterRule>> getEnabledRules() {
        return ResponseEntity.ok(filterRuleService.getEnabledRules());
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<FilterRule> toggleRule(@PathVariable String id,
                                                  @RequestParam boolean enabled) {
        return ResponseEntity.ok(filterRuleService.toggleRule(id, enabled));
    }

    @PostMapping("/test")
    public ResponseEntity<RuleTestResponse> testRules(@RequestBody RuleTestRequest request) {
        List<FilterRule> matchingRules = ruleEvaluationService.findMatchingRules(
                request.getEventPayload(), request.getTopic());

        int totalRules = filterRuleService.getEnabledRules().size();

        RuleTestResponse response = RuleTestResponse.builder()
                .matched(!matchingRules.isEmpty())
                .matchingRules(matchingRules)
                .totalRulesEvaluated(totalRules)
                .build();

        return ResponseEntity.ok(response);
    }

    private FilterRule mapToEntity(FilterRuleRequest request) {
        return FilterRule.builder()
                .name(request.getName())
                .description(request.getDescription())
                .topics(request.getTopics())
                .column1(request.getColumn1())
                .value1(request.getValue1())
                .column2(request.getColumn2())
                .value2(request.getValue2())
                .column3(request.getColumn3())
                .value3(request.getValue3())
                .column4(request.getColumn4())
                .value4(request.getValue4())
                .column5(request.getColumn5())
                .value5(request.getValue5())
                .column6(request.getColumn6())
                .value6(request.getValue6())
                .column7(request.getColumn7())
                .value7(request.getValue7())
                .action(request.getAction())
                .targetTopic(request.getTargetTopic())
                .priority(request.getPriority())
                .enabled(request.isEnabled())
                .build();
    }
}
