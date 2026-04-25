package com.eventfilter.service;

import com.eventfilter.model.ColumnMapping;
import com.eventfilter.repository.ColumnMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ColumnMappingService {

    private final ColumnMappingRepository mappingRepository;

    public ColumnMapping create(ColumnMapping mapping) {
        if (mappingRepository.findByColumnName(mapping.getColumnName()).isPresent()) {
            throw new IllegalArgumentException(
                    "Mapping for column '" + mapping.getColumnName() + "' already exists");
        }
        ColumnMapping saved = mappingRepository.save(mapping);
        log.info("Created column mapping: {} → {}", saved.getColumnName(), saved.getPayloadPath());
        return saved;
    }

    public ColumnMapping update(String id, ColumnMapping mapping) {
        ColumnMapping existing = mappingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mapping not found: " + id));

        existing.setColumnName(mapping.getColumnName());
        existing.setPayloadPath(mapping.getPayloadPath());
        existing.setDescription(mapping.getDescription());

        ColumnMapping saved = mappingRepository.save(existing);
        log.info("Updated column mapping: {} → {}", saved.getColumnName(), saved.getPayloadPath());
        return saved;
    }

    public void delete(String id) {
        mappingRepository.deleteById(id);
        log.info("Deleted column mapping: {}", id);
    }

    public List<ColumnMapping> getAll() {
        return mappingRepository.findAll();
    }

    /**
     * Returns a map of columnName → payloadPath for all configured mappings.
     */
    public Map<String, String> getMappingLookup() {
        return mappingRepository.findAll().stream()
                .collect(Collectors.toMap(
                        ColumnMapping::getColumnName,
                        ColumnMapping::getPayloadPath));
    }
}
