package com.eventfilter.config;

import com.eventfilter.engine.RuleEngine;
import com.eventfilter.model.FilterRule;
import com.eventfilter.repository.FilterRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:event-filter-group}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, Map<String, Object>> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "java.util.HashMap");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Container factory with RecordFilterStrategy.
     * Messages that don't match ANY enabled rule are discarded BEFORE reaching the listener.
     * Each rule carries its own payload paths — no external mapping needed.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Map<String, Object>> kafkaListenerContainerFactory(
            FilterRuleRepository ruleRepository,
            RuleEngine ruleEngine) {

        ConcurrentKafkaListenerContainerFactory<String, Map<String, Object>> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);

        factory.setRecordFilterStrategy(record -> {
            Map<String, Object> payload = record.value();
            String topic = record.topic();

            List<FilterRule> enabledRules = ruleRepository.findByEnabledTrueOrderByPriorityAsc();

            for (FilterRule rule : enabledRules) {
                if (ruleEngine.evaluate(rule, payload, topic)) {
                    log.debug("Record on topic '{}' matched rule '{}' — keeping", topic, rule.getName());
                    return false; // KEEP
                }
            }

            log.debug("Record on topic '{}' matched no rules — discarding before listener", topic);
            return true; // DISCARD
        });

        return factory;
    }
}
