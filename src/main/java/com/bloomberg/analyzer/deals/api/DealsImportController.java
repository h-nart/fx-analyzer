package com.bloomberg.analyzer.deals.api;

import com.bloomberg.analyzer.deals.api.dto.DealImportRequest;
import com.bloomberg.analyzer.deals.api.dto.DealsImportResponse;
import com.bloomberg.analyzer.deals.service.DealImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/v1/deals", produces = MediaType.APPLICATION_JSON_VALUE)
public class DealsImportController {
    private final DealImportService dealImportService;

    @PostMapping(path = "/import", consumes = MediaType.APPLICATION_JSON_VALUE)
    public DealsImportResponse importDeals(@RequestBody List<DealImportRequest> deals) {
        return dealImportService.importDeals(deals);
    }
}


