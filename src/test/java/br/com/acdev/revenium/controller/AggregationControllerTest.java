package br.com.acdev.revenium.controller;

import br.com.acdev.revenium.domain.Aggregations;
import br.com.acdev.revenium.service.AggregationWindowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AggregationController.class)
class AggregationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AggregationWindowService aggregationWindowService;

    @Test
    void getCurrent_noContent() throws Exception {
        when(aggregationWindowService.getCurrentAggregations(any(UUID.class), any(UUID.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/aggregations/current")
                        .param("tenantId", UUID.randomUUID().toString())
                        .param("customerId", UUID.randomUUID().toString()))
                .andExpect(status().isNoContent());
    }

    @Test
    void getCurrent_ok() throws Exception {
        Aggregations.SubAggregation epA = Aggregations.SubAggregation.builder()
                .calls(BigInteger.valueOf(2))
                .tokens(BigInteger.valueOf(20))
                .build();
        Aggregations.SubAggregation m1 = Aggregations.SubAggregation.builder()
                .calls(BigInteger.valueOf(3))
                .tokens(BigInteger.valueOf(30))
                .build();

        Aggregations ag = Aggregations.builder()
                .totalCalls(BigInteger.valueOf(3))
                .totalTokens(BigInteger.valueOf(30))
                .totalInputTokens(BigInteger.valueOf(12))
                .totalOutputTokens(BigInteger.valueOf(18))
                .avgLatencyMs(new BigDecimal("250.00"))
                .byEndpoint(Map.of("/a", epA))
                .byModel(Map.of("m1", m1))
                .build();

        when(aggregationWindowService.getCurrentAggregations(any(UUID.class), any(UUID.class)))
                .thenReturn(Optional.of(ag));

        mockMvc.perform(get("/aggregations/current")
                        .param("tenantId", UUID.randomUUID().toString())
                        .param("customerId", UUID.randomUUID().toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalCalls").value(3))
                .andExpect(jsonPath("$.totalTokens").value(30))
                .andExpect(jsonPath("$.totalInputTokens").value(12))
                .andExpect(jsonPath("$.totalOutputTokens").value(18))
                .andExpect(jsonPath("$.avgLatencyMs").value(250.00))
                .andExpect(jsonPath("$.byEndpoint['/a'].calls").value(2))
                .andExpect(jsonPath("$.byEndpoint['/a'].tokens").value(20))
                .andExpect(jsonPath("$.byModel.m1.calls").value(3))
                .andExpect(jsonPath("$.byModel.m1.tokens").value(30));
    }
}

