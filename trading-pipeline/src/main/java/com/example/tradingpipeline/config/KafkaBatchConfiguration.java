package com.example.tradingpipeline.config;

import com.example.tradingpipeline.model.TradeEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

@Configuration
public class KafkaBatchConfiguration {

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, TradeEvent> batchKafkaListenerContainerFactory(
            ConsumerFactory<String, TradeEvent> consumerFactory,
            PipelineProperties properties) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, TradeEvent>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        factory.setConcurrency(properties.listenerConcurrency());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
}