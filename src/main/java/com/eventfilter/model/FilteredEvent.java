package com.eventfilter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "filtered_events")
public class FilteredEvent {

    @Id
    private String id;

    private String sourceTopic;
    private String matchedRuleId;
    private String matchedRuleName;
    private RuleAction actionTaken;
    private String targetTopic;
    private Map<String, Object> eventPayload;
    private Map<String, String> headers;

    @CreatedDate
    private Instant processedAt;
}
