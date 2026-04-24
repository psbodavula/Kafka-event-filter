package com.eventfilter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldCondition {

    /**
     * The operator: EQUALS, NOT_EQUALS, CONTAINS, REGEX, GREATER_THAN, LESS_THAN, IN, NOT_IN, EXISTS
     */
    private ConditionOperator operator;

    /**
     * The value to compare against.
     */
    private Object value;
}
