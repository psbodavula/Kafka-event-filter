package com.eventfilter.dto;

import com.eventfilter.model.RuleAction;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterRuleRequest {

    @NotBlank(message = "Rule name is required")
    private String name;

    private String description;

    private List<String> topics;

    // Column (payload path) / Value pairs
    private String column1;
    private String value1;

    private String column2;
    private String value2;

    private String column3;
    private String value3;

    @Builder.Default
    private RuleAction action = RuleAction.FORWARD;

    private String targetTopic;

    @Builder.Default
    private int priority = 100;

    @Builder.Default
    private boolean enabled = true;
}
