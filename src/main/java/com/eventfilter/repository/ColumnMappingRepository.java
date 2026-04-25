package com.eventfilter.repository;

import com.eventfilter.model.ColumnMapping;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ColumnMappingRepository extends MongoRepository<ColumnMapping, String> {

    Optional<ColumnMapping> findByColumnName(String columnName);
}
