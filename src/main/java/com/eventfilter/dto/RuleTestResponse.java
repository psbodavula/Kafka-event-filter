package com.eventfilter.dto;

import com.eventfilter.model.FilterRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleTestResponse {

    private boolean matched;
    private List<FilterRule> matchingRules;
    private int totalRulesEvaluated;
}
