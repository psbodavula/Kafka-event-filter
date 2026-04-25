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

    // Column-value conditions (up to 7)
    private String column1;
    private String value1;

    private String column2;
    private String value2;

    private String column3;
    private String value3;

    private String column4;
    private String value4;

    private String column5;
    private String value5;

    private String column6;
    private String value6;

    private String column7;
    private String value7;

    @Builder.Default
    private RuleAction action = RuleAction.FORWARD;

    private String targetTopic;

    @Builder.Default
    private int priority = 100;

    @Builder.Default
    private boolean enabled = true;
}
