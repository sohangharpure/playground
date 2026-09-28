package com.example.tradingpipeline.service;

import com.example.tradingpipeline.model.TradeEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TradePersistenceListener {
    private final TradeProjectionService projectionService;

    public TradePersistenceListener(TradeProjectionService projectionService) {
        this.projectionService = projectionService;
    }

    @KafkaListener(
            topics = "${pipeline.topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "batchKafkaListenerContainerFactory")
    public void persist(List<ConsumerRecord<String, TradeEvent>> records, Acknowledgment acknowledgment) {
        projectionService.persist(records.stream().map(ConsumerRecord::value).toList());
        acknowledgment.acknowledge();
    }
}