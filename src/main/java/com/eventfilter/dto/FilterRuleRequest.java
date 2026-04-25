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

    /** Matches wfEvtInf/evtApplid */
    private String evtApplid;

    /** Matches wfEvtInf/evtNm */
    private String evtNm;

    /** Matches wfPmtOrdrPrcg/srcChnl */
    private String srcChnl;

    @Builder.Default
    private RuleAction action = RuleAction.FORWARD;

    private String targetTopic;

    @Builder.Default
    private int priority = 100;

    @Builder.Default
    private boolean enabled = true;
}
