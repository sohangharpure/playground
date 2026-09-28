package com.example.tradingpipeline.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pipeline")
public record PipelineProperties(
        String topic,
        String mongoCollection,
        int listenerConcurrency) {
}