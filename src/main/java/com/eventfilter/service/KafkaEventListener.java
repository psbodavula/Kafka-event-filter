package com.eventfilter.service;

import com.eventfilter.converter.mapper.DataDrivenGpiMapper;
import com.eventfilter.swift.model.GpiStatusUpdateResponse;
import com.eventfilter.swift.service.SwiftGpiTrackerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Only receives messages that already passed the RecordFilterStrategy (rule matching).
 * This listener focuses on: audit logging, event processing, and SWIFT GPI tracker updates
 * via the data-driven mapping pipeline.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventListener {

    private final EventProcessingService eventProcessingService;
    private final DataDrivenGpiMapper dataDrivenGpiMapper;
    private final SwiftGpiTrackerService swiftGpiTrackerService;

    @KafkaListener(topicPattern = "${kafka.topic-pattern:events.*}", groupId = "${spring.kafka.consumer.group-id:event-filter-group}")
    public void onEvent(ConsumerRecord<String, Map<String, Object>> record) {
        String topic = record.topic();
        Map<String, Object> payload = record.value();
        Map<String, String> headers = extractHeaders(record);

        log.info("Processing filtered event on topic '{}': key={}", topic, record.key());

        try {
            // 1. Execute rule actions (forward/route/drop + audit trail)
            eventProcessingService.processEvent(topic, payload, headers);

            // 2. Use data-driven mapper to check for UETR and call SWIFT GPI Tracker
            String uetr = dataDrivenGpiMapper.extractUetr(payload);

            if (uetr != null && !uetr.isBlank()) {
                log.info("UETR found: {}, invoking data-driven GPI Tracker pipeline", uetr);
                GpiStatusUpdateResponse gpiResponse = swiftGpiTrackerService.updatePaymentStatusDataDriven(payload);

                if (gpiResponse.isSuccess()) {
                    log.info("GPI Tracker updated for UETR: {}, confirmation: {}",
                            uetr, gpiResponse.getConfirmationNumber());
                } else {
                    log.warn("GPI Tracker update failed for UETR: {}: {}",
                            uetr, gpiResponse.getErrorMessage());
                }
            } else {
                log.debug("No UETR in payload, skipping GPI tracker update for topic '{}'", topic);
            }

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
