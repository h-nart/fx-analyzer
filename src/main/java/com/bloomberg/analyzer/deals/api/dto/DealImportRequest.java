package com.bloomberg.analyzer.deals.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

public record DealImportRequest(
        @NotBlank String dealId,
        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$", message = "must be ISO 4217 alpha-3 (A-Z)")
        String fromCurrency,
        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$", message = "must be ISO 4217 alpha-3 (A-Z)")
        String toCurrency,
        @NotNull Instant timestamp,
        @NotNull @Positive BigDecimal amount
) {
}


