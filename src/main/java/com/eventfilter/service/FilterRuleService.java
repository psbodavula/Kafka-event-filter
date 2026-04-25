package com.eventfilter.service;

import com.eventfilter.model.FilterRule;
import com.eventfilter.repository.FilterRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilterRuleService {

    private final FilterRuleRepository ruleRepository;

    public FilterRule createRule(FilterRule rule) {
        if (ruleRepository.findByName(rule.getName()).isPresent()) {
            throw new IllegalArgumentException("Rule with name '" + rule.getName() + "' already exists");
        }
        FilterRule saved = ruleRepository.save(rule);
        log.info("Created filter rule: {} (id={})", saved.getName(), saved.getId());
        return saved;
    }

    public FilterRule updateRule(String id, FilterRule rule) {
        FilterRule existing = ruleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + id));

        existing.setName(rule.getName());
        existing.setDescription(rule.getDescription());
        existing.setTopics(rule.getTopics());
        existing.setEvtApplid(rule.getEvtApplid());
        existing.setEvtNm(rule.getEvtNm());
        existing.setSrcChnl(rule.getSrcChnl());
        existing.setAction(rule.getAction());
        existing.setTargetTopic(rule.getTargetTopic());
        existing.setPriority(rule.getPriority());
        existing.setEnabled(rule.isEnabled());

        FilterRule saved = ruleRepository.save(existing);
        log.info("Updated filter rule: {} (id={})", saved.getName(), saved.getId());
        return saved;
    }

    public void deleteRule(String id) {
        ruleRepository.deleteById(id);
        log.info("Deleted filter rule: {}", id);
    }

    public Optional<FilterRule> getRule(String id) {
        return ruleRepository.findById(id);
    }

    public List<FilterRule> getAllRules() {
        return ruleRepository.findAll();
    }

    public List<FilterRule> getEnabledRules() {
        return ruleRepository.findByEnabledTrueOrderByPriorityAsc();
    }

    public FilterRule toggleRule(String id, boolean enabled) {
        FilterRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + id));
        rule.setEnabled(enabled);
        return ruleRepository.save(rule);
    }
}
