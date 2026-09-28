package com.example.tradingpipeline.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TradeEventTest {

    @Test
    void keyUsesTheProjectionIdentityFields() {
        var event = new TradeEvent(
                "event-1",
                "AAPL",
                LocalDate.parse("2026-09-25"),
                "sample-feed",
                new BigDecimal("227.45"),
                100,
                Instant.parse("2026-09-25T12:00:00Z"));

        assertThat(event.key()).isEqualTo("AAPL|2026-09-25|sample-feed");
    }
}