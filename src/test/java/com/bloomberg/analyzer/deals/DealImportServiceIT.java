package com.bloomberg.analyzer.deals;

import com.bloomberg.analyzer.deals.api.dto.DealImportRequest;
import com.bloomberg.analyzer.deals.api.dto.DealRowResult;
import com.bloomberg.analyzer.deals.api.dto.DealsImportResponse;
import com.bloomberg.analyzer.deals.persistence.DealRepository;
import com.bloomberg.analyzer.deals.service.DealImportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@SuppressWarnings("resource")
class DealImportServiceIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("analyzer")
            .withUsername("analyzer")
            .withPassword("analyzer");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    DealImportService dealImportService;

    @Autowired
    DealRepository dealRepository;

    @Test
    void import_isIdempotentAndNoRollback() {
        DealImportRequest good1 = new DealImportRequest("D-10001", "USD", "EUR", Instant.parse("2026-01-05T10:15:30Z"), new BigDecimal("10.00"));
        DealImportRequest badAmount = new DealImportRequest("D-10002", "USD", "EUR", Instant.parse("2026-01-05T10:15:31Z"), new BigDecimal("-1.00"));
        DealImportRequest goodDup = new DealImportRequest("D-10001", "USD", "EUR", Instant.parse("2026-01-05T10:15:32Z"), new BigDecimal("20.00"));

        DealsImportResponse response = dealImportService.importDeals(List.of(good1, badAmount, goodDup));

        assertThat(response.total()).isEqualTo(3);
        assertThat(response.imported()).isEqualTo(1);
        assertThat(response.failedValidation()).isEqualTo(1);
        assertThat(response.duplicates()).isEqualTo(1);

        assertThat(response.rows()).hasSize(3);
        assertThat(response.rows().get(0).status()).isEqualTo(DealRowResult.Status.IMPORTED);
        assertThat(response.rows().get(1).status()).isEqualTo(DealRowResult.Status.VALIDATION_FAILED);
        assertThat(response.rows().get(2).status()).isEqualTo(DealRowResult.Status.DUPLICATE);

        // No rollback: the first successful row should still be persisted even though row #2 failed validation.
        assertThat(dealRepository.count()).isEqualTo(1);
        assertThat(dealRepository.existsByDealId("D-10001")).isTrue();
    }
}


