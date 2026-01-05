package com.bloomberg.analyzer.deals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bloomberg.analyzer.common.error.GlobalExceptionHandler;
import com.bloomberg.analyzer.deals.api.DealsImportController;
import com.bloomberg.analyzer.deals.api.dto.DealRowResult;
import com.bloomberg.analyzer.deals.api.dto.DealsImportResponse;
import com.bloomberg.analyzer.deals.service.DealImportService;

import java.util.List;

@WebMvcTest(controllers = DealsImportController.class)
@Import(GlobalExceptionHandler.class)
class DealsImportControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DealImportService dealImportService;

    @Test
    void importDeals_returnsResponseFromService() throws Exception {
        DealsImportResponse resp = new DealsImportResponse(
                1, 1, 0, 0, 0,
                List.of(new DealRowResult(0, "D-1", DealRowResult.Status.IMPORTED, List.of()))
        );
        when(dealImportService.importDeals(anyList())).thenReturn(resp);

        String body = """
                [
                  { "dealId":"D-1","fromCurrency":"USD","toCurrency":"EUR","timestamp":"2026-01-05T10:15:30Z","amount":"1.00" }
                ]
                """;

        mockMvc.perform(post("/api/v1/deals/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.rows[0].status").value("IMPORTED"));
    }

    @Test
    void importDeals_invalidJson_returns400ProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/deals/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }
}


