package com.example.tradingpipeline.service;

import com.example.tradingpipeline.config.PipelineProperties;
import com.example.tradingpipeline.model.TradeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TradePublisher {
    private static final Logger log = LoggerFactory.getLogger(TradePublisher.class);

    private final KafkaTemplate<String, TradeEvent> kafkaTemplate;
    private final PipelineProperties properties;

    public TradePublisher(KafkaTemplate<String, TradeEvent> kafkaTemplate, PipelineProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    public TradeEvent publish(TradeEvent event) {
        kafkaTemplate.send(properties.topic(), event.key(), event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Kafka publish failed for key={}", event.key(), exception);
                    } else {
                        log.debug("Published key={} to partition={}", event.key(), result.getRecordMetadata().partition());
                    }
                });
        return event;
    }
}