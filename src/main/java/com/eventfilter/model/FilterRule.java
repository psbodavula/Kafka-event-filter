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
 * Simple table-based filter rule.
 * Each rule defines up to 7 column-value conditions.
 * An event matches when ALL non-null columns equal the event's corresponding field values.
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

    // --- Column-value conditions (if column is set, event field must equal value) ---

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
     * Returns all non-null column-value pairs as an ordered map.
     */
    public Map<String, String> getConditions() {
        Map<String, String> conditions = new LinkedHashMap<>();
        addIfPresent(conditions, column1, value1);
        addIfPresent(conditions, column2, value2);
        addIfPresent(conditions, column3, value3);
        addIfPresent(conditions, column4, value4);
        addIfPresent(conditions, column5, value5);
        addIfPresent(conditions, column6, value6);
        addIfPresent(conditions, column7, value7);
        return conditions;
    }

    private void addIfPresent(Map<String, String> map, String column, String value) {
        if (column != null && !column.isBlank()) {
            map.put(column, value);
        }
    }
}
