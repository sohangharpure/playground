package com.example.tradingpipeline.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TradeRequest(
        @NotBlank String securityId,
        @NotNull LocalDate tradeDate,
        @NotBlank String source,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        @Positive long quantity) {
}