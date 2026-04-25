package com.eventfilter.controller;

import com.eventfilter.model.ColumnMapping;
import com.eventfilter.service.ColumnMappingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mappings")
@RequiredArgsConstructor
public class ColumnMappingController {

    private final ColumnMappingService mappingService;

    @PostMapping
    public ResponseEntity<ColumnMapping> create(@Valid @RequestBody ColumnMapping mapping) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mappingService.create(mapping));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ColumnMapping> update(@PathVariable String id,
                                                 @Valid @RequestBody ColumnMapping mapping) {
        return ResponseEntity.ok(mappingService.update(id, mapping));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        mappingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<ColumnMapping>> getAll() {
        return ResponseEntity.ok(mappingService.getAll());
    }

    @GetMapping("/lookup")
    public ResponseEntity<Map<String, String>> getLookup() {
        return ResponseEntity.ok(mappingService.getMappingLookup());
    }
}
