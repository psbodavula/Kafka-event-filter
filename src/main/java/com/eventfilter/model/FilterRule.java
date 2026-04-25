package com.eventfilter.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Table-based filter rule with named conditions.
 * Matches event payload paths:
 *   evtApplid  → wfEvtInf/evtApplid
 *   evtNm      → wfEvtInf/evtNm
 *   srcChnl    → wfPmtOrdrPrcg/srcChnl
 *
 * An event matches when ALL non-null fields equal the event's corresponding nested values.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "filter_rules")
public class FilterRule {

    @Id
    private String id;

    @NotBlank(message = "Rule name is required")
    @Indexed(unique = true)
    private String name;

    private String description;

    /** Topics this rule applies to. Empty/null means all topics. */
    private List<String> topics;

    // --- Named conditions mapped to event payload paths ---

    /** Matches wfEvtInf/evtApplid in the event payload */
    private String evtApplid;

    /** Matches wfEvtInf/evtNm in the event payload */
    private String evtNm;

    /** Matches wfPmtOrdrPrcg/srcChnl in the event payload */
    private String srcChnl;

    /** Action to take when rule matches: FORWARD, DROP, ROUTE */
    @Builder.Default
    private RuleAction action = RuleAction.FORWARD;

    /** Target topic for FORWARD/ROUTE actions. */
    private String targetTopic;

    /** Priority (lower = higher priority). */
    @Builder.Default
    private int priority = 100;

    @Builder.Default
    private boolean enabled = true;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    /**
     * Returns all non-null conditions as a map of (payload path → expected value).
     */
    public Map<String, String> getConditions() {
        Map<String, String> conditions = new LinkedHashMap<>();
        if (evtApplid != null && !evtApplid.isBlank()) {
            conditions.put("wfEvtInf/evtApplid", evtApplid);
        }
        if (evtNm != null && !evtNm.isBlank()) {
            conditions.put("wfEvtInf/evtNm", evtNm);
        }
        if (srcChnl != null && !srcChnl.isBlank()) {
            conditions.put("wfPmtOrdrPrcg/srcChnl", srcChnl);
        }
        return conditions;
    }
}
