package com.eventfilter.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Maps a rule column name to an event payload path.
 * Stored in MongoDB so mappings are fully configurable without code changes.
 *
 * Example:
 *   columnName  = "evtApplid"
 *   payloadPath = "wfEvtInf/evtApplid"
 *
 * The rule engine uses this to resolve: rule.evtApplid → payload["wfEvtInf"]["evtApplid"]
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "column_mappings")
public class ColumnMapping {

    @Id
    private String id;

    @NotBlank(message = "Column name is required")
    @Indexed(unique = true)
    private String columnName;

    @NotBlank(message = "Payload path is required")
    private String payloadPath;

    private String description;
}
