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
 * Filter rule with named condition fields.
 * The mapping from field names to event payload paths is stored in the column_mappings collection.
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

    // --- Named conditions (field-to-path mapping comes from column_mappings DB) ---

    private String evtApplid;

    private String evtNm;

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
     * Returns non-null conditions as a map of (payload path → expected value),
     * using the column mapping lookup from the database.
     *
     * @param columnMappings map of columnName → payloadPath from column_mappings collection
     */
    public Map<String, String> getConditions(Map<String, String> columnMappings) {
        Map<String, String> conditions = new LinkedHashMap<>();
        addIfPresent(conditions, "evtApplid", evtApplid, columnMappings);
        addIfPresent(conditions, "evtNm", evtNm, columnMappings);
        addIfPresent(conditions, "srcChnl", srcChnl, columnMappings);
        return conditions;
    }

    private void addIfPresent(Map<String, String> conditions, String columnName,
                               String value, Map<String, String> columnMappings) {
        if (value != null && !value.isBlank()) {
            String payloadPath = columnMappings.getOrDefault(columnName, columnName);
            conditions.put(payloadPath, value);
        }
    }
}
