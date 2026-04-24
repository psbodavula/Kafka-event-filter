package com.eventfilter.dto;

import com.eventfilter.model.FieldCondition;
import com.eventfilter.model.LogicalOperator;
import com.eventfilter.model.RuleAction;
import com.eventfilter.model.RuleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterRuleRequest {

    @NotBlank(message = "Rule name is required")
    private String name;

    private String description;

    private List<String> topics;

    @NotNull(message = "Rule type is required")
    private RuleType ruleType;

    private String spelExpression;

    private Map<String, FieldCondition> fieldConditions;

    private List<String> childRuleIds;
    private LogicalOperator compositeOperator;

    @Builder.Default
    private RuleAction action = RuleAction.FORWARD;

    private String targetTopic;

    @Builder.Default
    private int priority = 100;

    @Builder.Default
    private boolean enabled = true;
}
