package com.example.tradingpipeline.api;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.tradingpipeline.model.TradeDocument;
import com.example.tradingpipeline.service.TradeProjectionService;

@RestController
@RequestMapping("/api/v1/trades")
public class HistoricalTradeController {
    private final TradeProjectionService projectionService;

    public HistoricalTradeController(TradeProjectionService projectionService) {
        this.projectionService = projectionService;
    }

    @GetMapping("/{securityId}")
    public List<TradeDocument> history(
            @PathVariable String securityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate to) {
        return projectionService.findTrades(securityId, from, to);
    }
}