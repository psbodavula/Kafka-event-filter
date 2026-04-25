package com.eventfilter.repository;

import com.eventfilter.model.FilterRule;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FilterRuleRepository extends MongoRepository<FilterRule, String> {

    List<FilterRule> findByEnabledTrueOrderByPriorityAsc();

    List<FilterRule> findByTopicsContainingAndEnabledTrueOrderByPriorityAsc(String topic);

    Optional<FilterRule> findByName(String name);
}
