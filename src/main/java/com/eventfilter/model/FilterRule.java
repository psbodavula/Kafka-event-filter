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
import java.util.List;
import java.util.Map;

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

    /**
     * Topics this rule applies to. Empty/null means all topics.
     */
    private List<String> topics;

    /**
     * The type of rule: SPEL, JSON_PATH, FIELD_MATCH, COMPOSITE
     */
    @NotBlank(message = "Rule type is required")
    private RuleType ruleType;

    /**
     * SpEL expression for SPEL type rules.
     * e.g. "#event['severity'] == 'CRITICAL' && #event['source'] == 'payment-service'"
     */
    private String spelExpression;

    /**
     * Field-based conditions for FIELD_MATCH type rules.
     * Key = field name (supports dot notation), Value = expected value or pattern.
     */
    private Map<String, FieldCondition> fieldConditions;

    /**
     * For COMPOSITE rules: list of child rule IDs combined with a logical operator.
     */
    private List<String> childRuleIds;
    private LogicalOperator compositeOperator;

    /**
     * Action to take when rule matches: FORWARD, DROP, TRANSFORM, ROUTE
     */
    @Builder.Default
    private RuleAction action = RuleAction.FORWARD;

    /**
     * Target topic for FORWARD/ROUTE actions.
     */
    private String targetTopic;

    /**
     * Priority (lower = higher priority). Rules are evaluated in priority order.
     */
    @Builder.Default
    private int priority = 100;

    @Builder.Default
    private boolean enabled = true;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
