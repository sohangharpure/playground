package com.example.tradingpipeline.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record TradeDocument(
        String id,
        String eventId,
        String securityId,
        LocalDate tradeDate,
        String source,
        BigDecimal price,
        long quantity,
        Instant eventTime) {

    public static TradeDocument from(TradeEvent event) {
        return new TradeDocument(
                event.key(),
                event.eventId(),
                event.securityId(),
                event.tradeDate(),
                event.source(),
                event.price(),
                event.quantity(),
                event.eventTime());
    }
}