package com.eventfilter.repository;

import com.eventfilter.model.FilteredEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FilteredEventRepository extends MongoRepository<FilteredEvent, String> {

    List<FilteredEvent> findBySourceTopicOrderByProcessedAtDesc(String sourceTopic);

    List<FilteredEvent> findByMatchedRuleIdOrderByProcessedAtDesc(String ruleId);

    List<FilteredEvent> findTop100ByOrderByProcessedAtDesc();
}
