package com.bloomberg.analyzer.deals.api.dto;

import java.util.List;

public record DealsImportResponse(
        int total,
        int imported,
        int duplicates,
        int failedValidation,
        int failedPersistence,
        List<DealRowResult> rows
) {
}


