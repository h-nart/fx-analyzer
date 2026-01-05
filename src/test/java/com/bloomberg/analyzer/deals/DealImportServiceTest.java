package com.bloomberg.analyzer.deals;

import com.bloomberg.analyzer.deals.api.dto.DealImportRequest;
import com.bloomberg.analyzer.deals.api.dto.DealRowResult;
import com.bloomberg.analyzer.deals.api.dto.DealsImportResponse;
import com.bloomberg.analyzer.deals.persistence.DealRepository;
import com.bloomberg.analyzer.deals.service.DealImportService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DealImportServiceTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void importDeals_nullList_returnsEmptyResponse() {
        DealRepository repo = mock(DealRepository.class);
        DealImportService service = new DealImportService(repo, validator);

        DealsImportResponse resp = service.importDeals(null);

        assertThat(resp.total()).isZero();
        assertThat(resp.imported()).isZero();
        assertThat(resp.duplicates()).isZero();
        assertThat(resp.failedValidation()).isZero();
        assertThat(resp.failedPersistence()).isZero();
        assertThat(resp.rows()).isEmpty();
        verifyNoInteractions(repo);
    }

    @Test
    void importDeals_nullRow_isValidationFailed() {
        DealRepository repo = mock(DealRepository.class);
        DealImportService service = new DealImportService(repo, validator);

        DealsImportResponse resp = service.importDeals(java.util.Arrays.asList((DealImportRequest) null));

        assertThat(resp.total()).isEqualTo(1);
        assertThat(resp.failedValidation()).isEqualTo(1);
        assertThat(resp.rows()).hasSize(1);
        assertThat(resp.rows().get(0).status()).isEqualTo(DealRowResult.Status.VALIDATION_FAILED);
        assertThat(resp.rows().get(0).errors()).contains("row is null");
        verifyNoInteractions(repo);
    }

    @Test
    void importDeals_missingFields_isValidationFailed() {
        DealRepository repo = mock(DealRepository.class);
        DealImportService service = new DealImportService(repo, validator);

        DealImportRequest req = new DealImportRequest(
                "D-30001",
                "USD",
                "EUR",
                null,
                null
        );

        DealsImportResponse resp = service.importDeals(List.of(req));

        assertThat(resp.total()).isEqualTo(1);
        assertThat(resp.failedValidation()).isEqualTo(1);
        assertThat(resp.rows()).hasSize(1);
        assertThat(resp.rows().get(0).errors())
                .anyMatch(e -> e.startsWith("timestamp:"))
                .anyMatch(e -> e.startsWith("amount:"));
        verifyNoInteractions(repo);
    }

    @Test
    void importDeals_unknownCurrency_isValidationFailed() {
        DealRepository repo = mock(DealRepository.class);
        DealImportService service = new DealImportService(repo, validator);

        DealImportRequest req = new DealImportRequest(
                "D-30002",
                "ZZZ",
                "EUR",
                Instant.parse("2026-01-05T10:15:30Z"),
                new BigDecimal("1.00")
        );

        DealsImportResponse resp = service.importDeals(List.of(req));

        assertThat(resp.failedValidation()).isEqualTo(1);
        assertThat(resp.rows().get(0).errors()).contains("fromCurrency: unknown ISO currency");
        verifyNoInteractions(repo);
    }

    @Test
    void importDeals_uniqueConstraintViolation_isDuplicate() {
        DealRepository repo = mock(DealRepository.class);
        DealImportService service = new DealImportService(repo, validator);

        DealImportRequest req = new DealImportRequest(
                "D-40001",
                "USD",
                "EUR",
                Instant.parse("2026-01-05T10:15:30Z"),
                new BigDecimal("1.00")
        );

        when(repo.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("violates constraint uk_deals_deal_id"));

        DealsImportResponse resp = service.importDeals(List.of(req));

        assertThat(resp.total()).isEqualTo(1);
        assertThat(resp.imported()).isZero();
        assertThat(resp.duplicates()).isEqualTo(1);
        assertThat(resp.rows().get(0).status()).isEqualTo(DealRowResult.Status.DUPLICATE);
        verify(repo, times(1)).saveAndFlush(any());
    }

    @Test
    void importDeals_otherDataIntegrityViolation_isPersistenceFailed() {
        DealRepository repo = mock(DealRepository.class);
        DealImportService service = new DealImportService(repo, validator);

        DealImportRequest req = new DealImportRequest(
                "D-40002",
                "USD",
                "EUR",
                Instant.parse("2026-01-05T10:15:30Z"),
                new BigDecimal("1.00")
        );

        when(repo.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("some other constraint"));

        DealsImportResponse resp = service.importDeals(List.of(req));

        assertThat(resp.total()).isEqualTo(1);
        assertThat(resp.failedPersistence()).isEqualTo(1);
        assertThat(resp.rows().get(0).status()).isEqualTo(DealRowResult.Status.PERSISTENCE_FAILED);
        verify(repo, times(1)).saveAndFlush(any());
    }

    @Test
    void importDeals_runtimeException_isPersistenceFailed() {
        DealRepository repo = mock(DealRepository.class);
        DealImportService service = new DealImportService(repo, validator);

        DealImportRequest req = new DealImportRequest(
                "D-40003",
                "USD",
                "EUR",
                Instant.parse("2026-01-05T10:15:30Z"),
                new BigDecimal("1.00")
        );

        when(repo.saveAndFlush(any())).thenThrow(new RuntimeException("boom"));

        DealsImportResponse resp = service.importDeals(List.of(req));

        assertThat(resp.total()).isEqualTo(1);
        assertThat(resp.failedPersistence()).isEqualTo(1);
        assertThat(resp.rows().get(0).status()).isEqualTo(DealRowResult.Status.PERSISTENCE_FAILED);
        verify(repo, times(1)).saveAndFlush(any());
    }
}


