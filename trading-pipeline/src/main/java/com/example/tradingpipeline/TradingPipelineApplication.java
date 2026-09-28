package com.example.tradingpipeline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TradingPipelineApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradingPipelineApplication.class, args);
    }
}