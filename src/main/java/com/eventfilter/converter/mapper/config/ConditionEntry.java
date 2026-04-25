package com.eventfilter.converter.mapper.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A single condition branch within a CONDITIONAL mapping rule.
 * If the source value matches any of {@code matchValues}, the {@code outputValue} is used.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConditionEntry {

    private List<String> matchValues;

    private String outputValue;
}
