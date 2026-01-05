package com.bloomberg.analyzer.deals.api.dto;

import java.util.List;

public record DealRowResult(
        int index,
        String dealId,
        Status status,
        List<String> errors
) {
    public enum Status {
        IMPORTED,
        DUPLICATE,
        VALIDATION_FAILED,
        PERSISTENCE_FAILED
    }
}


