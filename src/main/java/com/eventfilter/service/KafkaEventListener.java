package com.eventfilter.service;

import com.eventfilter.converter.JsonToUpoConverter;
import com.eventfilter.model.upo.UniversalPaymentObject;
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
 * This listener focuses on: audit logging, UPO conversion, and SWIFT GPI tracker updates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventListener {

    private final EventProcessingService eventProcessingService;
    private final JsonToUpoConverter jsonToUpoConverter;
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

            // 2. Convert JSON to UPO and call SWIFT GPI Tracker
            UniversalPaymentObject upo = jsonToUpoConverter.convert(payload);

            if (upo.getUetr() != null && !upo.getUetr().isBlank()) {
                log.info("UPO converted for UETR: {}, calling SWIFT GPI Tracker", upo.getUetr());
                GpiStatusUpdateResponse gpiResponse = swiftGpiTrackerService.updatePaymentStatus(upo);

                if (gpiResponse.isSuccess()) {
                    log.info("GPI Tracker updated for UETR: {}, confirmation: {}",
                            upo.getUetr(), gpiResponse.getConfirmationNumber());
                } else {
                    log.warn("GPI Tracker update failed for UETR: {}: {}",
                            upo.getUetr(), gpiResponse.getErrorMessage());
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
