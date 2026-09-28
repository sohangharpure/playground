package com.example.tradingpipeline.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TradeEvent(
        String eventId,
        String securityId,
        LocalDate tradeDate,
        String source,
        BigDecimal price,
        long quantity,
        Instant eventTime) {

    public static TradeEvent from(TradeRequest request) {
        return new TradeEvent(
                UUID.randomUUID().toString(),
                request.securityId(),
                request.tradeDate(),
                request.source(),
                request.price(),
                request.quantity(),
                Instant.now());
    }

    public String key() {
        return securityId + "|" + tradeDate + "|" + source;
    }
}