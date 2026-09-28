package com.example.tradingpipeline.service;

import com.example.tradingpipeline.model.TradeEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class TradePersistenceListenerTest {

    @Test
    void acknowledgesOnlyAfterProjectionSucceeds() {
        var projectionService = mock(TradeProjectionService.class);
        var acknowledgment = mock(Acknowledgment.class);
        var event = event();
        var listener = new TradePersistenceListener(projectionService);

        listener.persist(List.of(record(event)), acknowledgment);

        var order = inOrder(projectionService, acknowledgment);
        order.verify(projectionService).persist(List.of(event));
        order.verify(acknowledgment).acknowledge();
    }

    @Test
    void doesNotAcknowledgeWhenProjectionFails() {
        var projectionService = mock(TradeProjectionService.class);
        var acknowledgment = mock(Acknowledgment.class);
        var event = event();
        whenProjectionFails(projectionService);
        var listener = new TradePersistenceListener(projectionService);

        assertThatThrownBy(() -> listener.persist(List.of(record(event)), acknowledgment))
                .isInstanceOf(IllegalStateException.class);

        verify(acknowledgment, never()).acknowledge();
    }

    private static TradeEvent event() {
        return new TradeEvent(
                "event-1",
                "AAPL",
                LocalDate.parse("2026-09-25"),
                "sample-feed",
                new BigDecimal("227.45"),
                100,
                Instant.parse("2026-09-25T12:00:00Z"));
    }

    private static ConsumerRecord<String, TradeEvent> record(TradeEvent event) {
        return new ConsumerRecord<>("trading-events", 0, 0L, event.key(), event);
    }

    private static void whenProjectionFails(TradeProjectionService projectionService) {
        org.mockito.Mockito.doThrow(new IllegalStateException("projection write failed"))
                .when(projectionService).persist(org.mockito.ArgumentMatchers.anyList());
    }
}