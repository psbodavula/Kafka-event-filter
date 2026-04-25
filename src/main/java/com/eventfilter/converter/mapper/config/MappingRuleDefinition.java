package com.eventfilter.converter.mapper.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Defines a single field-level mapping rule that the DataDrivenMappingEngine evaluates.
 *
 * <p>Rule types:
 * <ul>
 *   <li>{@code MAP_AS_IS} — copy value from {@code sourceXpath} to {@code targetField} unchanged</li>
 *   <li>{@code CONDITIONAL} — look up source value in {@code conditions} to determine output</li>
 *   <li>{@code HARDCODE} — always use {@code hardcodedValue}</li>
 *   <li>{@code EXPRESSION} — evaluate a built-in expression (e.g. {@code CURRENT_TIMESTAMP})</li>
 *   <li>{@code PRESENCE_CHECK} — check if {@code sourceXpath} exists, then apply nested {@code thenRules}</li>
 * </ul>
 *
 * <p>Presence values: {@code REQUIRED} ([1..1]), {@code OPTIONAL} ([0..1]).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MappingRuleDefinition {

    private String targetField;

    private String type;

    private String sourceXpath;

    private String presence;

    private String hardcodedValue;

    private String expression;

    private List<ConditionEntry> conditions;

    private String errorOnNoMatch;

    private String errorOnMissing;

    private String fallbackSourceXpath;

    private List<MappingRuleDefinition> thenRules;
}
