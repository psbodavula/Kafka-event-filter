package com.eventfilter.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventListener {

    private final EventProcessingService eventProcessingService;

    /**
     * Listens to all event topics using a regex pattern.
     * The pattern "events.*" matches any topic starting with "events".
     * You can modify this pattern to match your naming convention.
     */
    @KafkaListener(topicPattern = "${kafka.topic-pattern:events.*}", groupId = "${spring.kafka.consumer.group-id:event-filter-group}")
    public void onEvent(ConsumerRecord<String, Map<String, Object>> record) {
        String topic = record.topic();
        Map<String, Object> payload = record.value();
        Map<String, String> headers = extractHeaders(record);

        log.info("Received event on topic '{}': key={}", topic, record.key());
        log.debug("Event payload: {}", payload);

        try {
            eventProcessingService.processEvent(topic, payload, headers);
        } catch (Exception e) {
            log.error("Error processing event from topic '{}': {}", topic, e.getMessage(), e);
        }
    }

    private Map<String, String> extractHeaders(ConsumerRecord<String, Map<String, Object>> record) {
        Map<String, String> headers = new HashMap<>();
        for (Header header : record.headers()) {
            headers.put(header.key(), new String(header.value(), StandardCharsets.UTF_8));
        }
        headers.put("kafka_topic", record.topic());
        headers.put("kafka_partition", String.valueOf(record.partition()));
        headers.put("kafka_offset", String.valueOf(record.offset()));
        headers.put("kafka_timestamp", String.valueOf(record.timestamp()));
        if (record.key() != null) {
            headers.put("kafka_key", record.key());
        }
        return headers;
    }
}
