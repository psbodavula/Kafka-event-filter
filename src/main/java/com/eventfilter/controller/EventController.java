package com.eventfilter.controller;

import com.eventfilter.dto.EventPublishRequest;
import com.eventfilter.model.FilteredEvent;
import com.eventfilter.repository.FilteredEventRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final FilteredEventRepository filteredEventRepository;

    @PostMapping("/publish")
    public ResponseEntity<String> publishEvent(@Valid @RequestBody EventPublishRequest request) {
        kafkaTemplate.send(request.getTopic(), request.getKey(), request.getPayload());
        return ResponseEntity.ok("Event published to topic: " + request.getTopic());
    }

    @GetMapping("/filtered")
    public ResponseEntity<List<FilteredEvent>> getFilteredEvents() {
        return ResponseEntity.ok(filteredEventRepository.findTop100ByOrderByProcessedAtDesc());
    }

    @GetMapping("/filtered/topic/{topic}")
    public ResponseEntity<List<FilteredEvent>> getFilteredEventsByTopic(@PathVariable String topic) {
        return ResponseEntity.ok(filteredEventRepository.findBySourceTopicOrderByProcessedAtDesc(topic));
    }

    @GetMapping("/filtered/rule/{ruleId}")
    public ResponseEntity<List<FilteredEvent>> getFilteredEventsByRule(@PathVariable String ruleId) {
        return ResponseEntity.ok(filteredEventRepository.findByMatchedRuleIdOrderByProcessedAtDesc(ruleId));
    }
}
