package com.bloomberg.analyzer.deals.service;

import com.bloomberg.analyzer.deals.api.dto.DealImportRequest;
import com.bloomberg.analyzer.deals.api.dto.DealRowResult;
import com.bloomberg.analyzer.deals.api.dto.DealsImportResponse;
import com.bloomberg.analyzer.deals.persistence.DealEntity;
import com.bloomberg.analyzer.deals.persistence.DealRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class DealImportService {
    private static final String UNIQUE_CONSTRAINT_DEAL_ID = "uk_deals_deal_id";

    private final DealRepository dealRepository;
    private final Validator validator;

    /**
     * Imports deals row-by-row. This method intentionally does NOT wrap the entire batch in a single transaction,
     * ensuring valid rows are persisted even when some rows fail validation or persistence.
     */
    public DealsImportResponse importDeals(List<DealImportRequest> requests) {
        List<DealImportRequest> safeRequests = requests == null ? List.of() : requests;

        int imported = 0;
        int duplicates = 0;
        int failedValidation = 0;
        int failedPersistence = 0;

        List<DealRowResult> rows = new ArrayList<>(safeRequests.size());

        for (int i = 0; i < safeRequests.size(); i++) {
            DealImportRequest req = safeRequests.get(i);
            String dealId = req == null ? null : req.dealId();

            List<String> errors = validate(req);
            if (!errors.isEmpty()) {
                failedValidation++;
                rows.add(new DealRowResult(i, dealId, DealRowResult.Status.VALIDATION_FAILED, errors));
                log.warn("dealImport.validationFailed index={} dealId={} errors={}", i, dealId, errors);
                continue;
            }

            try {
                DealEntity entity = new DealEntity(
                        req.dealId(),
                        req.fromCurrency(),
                        req.toCurrency(),
                        req.timestamp(),
                        req.amount()
                );
                dealRepository.saveAndFlush(entity);
                imported++;
                rows.add(new DealRowResult(i, req.dealId(), DealRowResult.Status.IMPORTED, List.of()));
            } catch (DataIntegrityViolationException e) {
                if (isUniqueConstraintViolation(e, UNIQUE_CONSTRAINT_DEAL_ID)) {
                    duplicates++;
                    rows.add(new DealRowResult(i, req.dealId(), DealRowResult.Status.DUPLICATE, List.of("duplicate dealId")));
                    log.warn("dealImport.duplicate index={} dealId={}", i, req.dealId());
                } else {
                    failedPersistence++;
                    rows.add(new DealRowResult(i, req.dealId(), DealRowResult.Status.PERSISTENCE_FAILED, List.of("persistence error")));
                    log.error("dealImport.persistenceFailed index={} dealId={}", i, req.dealId(), e);
                }
            } catch (RuntimeException e) {
                failedPersistence++;
                rows.add(new DealRowResult(i, req.dealId(), DealRowResult.Status.PERSISTENCE_FAILED, List.of("persistence error")));
                log.error("dealImport.persistenceFailed index={} dealId={}", i, req.dealId(), e);
            }
        }

        log.info("dealImport.completed total={} imported={} duplicates={} failedValidation={} failedPersistence={}",
                safeRequests.size(), imported, duplicates, failedValidation, failedPersistence);

        return new DealsImportResponse(
                safeRequests.size(),
                imported,
                duplicates,
                failedValidation,
                failedPersistence,
                rows
        );
    }

    private List<String> validate(DealImportRequest req) {
        List<String> errors = new ArrayList<>();
        if (req == null) {
            errors.add("row is null");
            return errors;
        }

        Set<ConstraintViolation<DealImportRequest>> violations = validator.validate(req);
        for (ConstraintViolation<DealImportRequest> v : violations) {
            errors.add(v.getPropertyPath() + ": " + v.getMessage());
        }

        if (req.fromCurrency() != null && !isValidIsoCurrency(req.fromCurrency())) {
            errors.add("fromCurrency: unknown ISO currency");
        }
        if (req.toCurrency() != null && !isValidIsoCurrency(req.toCurrency())) {
            errors.add("toCurrency: unknown ISO currency");
        }

        return errors;
    }

    private boolean isValidIsoCurrency(String code) {
        try {
            Currency.getInstance(code);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isUniqueConstraintViolation(Throwable t, String constraintName) {
        Throwable cur = t;
        while (cur != null) {
            String msg = cur.getMessage();
            if (msg != null && msg.contains(constraintName)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }
}


