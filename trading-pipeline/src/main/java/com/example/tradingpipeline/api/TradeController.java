package com.example.tradingpipeline.api;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.tradingpipeline.model.TradeEvent;
import com.example.tradingpipeline.model.TradeRequest;
import com.example.tradingpipeline.service.TradePublisher;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/trades")
public class TradeController {
    private final TradePublisher publisher;

    public TradeController(TradePublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping
    public ResponseEntity<TradeEvent> ingest(@Valid @RequestBody TradeRequest request) {
        var event = publisher.publish(TradeEvent.from(request));
        return ResponseEntity.accepted()
                .location(URI.create("/api/v1/trades/" + event.securityId()))
                .body(event);
    }
}