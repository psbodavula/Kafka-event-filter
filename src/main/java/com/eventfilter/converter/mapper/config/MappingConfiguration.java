package com.eventfilter.converter.mapper.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Top-level mapping configuration loaded from a JSON resource file.
 * Contains metadata and the ordered list of mapping rules.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MappingConfiguration {

    private String mappingName;

    private String version;

    private String description;

    private List<MappingRuleDefinition> rules;
}
