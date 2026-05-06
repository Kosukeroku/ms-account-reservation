package kosukeroku.ms_account_reservation.controller;

import kosukeroku.ms_account_reservation.exception.ExternalServiceException;
import kosukeroku.ms_account_reservation.service.ExchangeRateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CurrencyController.class)
class CurrencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExchangeRateService exchangeRateService;

    @MockitoBean
    private io.micrometer.core.instrument.MeterRegistry meterRegistry;

    @Test
    void getRate_shouldReturn200_whenValidCurrencies() throws Exception {
        // given
        when(exchangeRateService.getExchangeRate("USD", "EUR")).thenReturn(new BigDecimal("0.86"));

        // then
        mockMvc.perform(get("/api/v1/rate?from=USD&to=EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("USD"))
                .andExpect(jsonPath("$.to").value("EUR"))
                .andExpect(jsonPath("$.rate").value(0.86));
    }

    @Test
    void getRate_shouldReturn400_whenFromCurrencyInvalid() throws Exception {
        // given
        when(exchangeRateService.getExchangeRate("XXX", "EUR"))
                .thenThrow(new IllegalArgumentException("Currency code not found: XXX"));

        // then
        mockMvc.perform(get("/api/v1/rate?from=XXX&to=EUR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorDescription").value("Currency code not found: XXX"));
    }

    @Test
    void getRate_shouldReturn400_whenToCurrencyInvalid() throws Exception {
        // given
        when(exchangeRateService.getExchangeRate("USD", "XXX"))
                .thenThrow(new IllegalArgumentException("Currency code not found: XXX"));

        // then
        mockMvc.perform(get("/api/v1/rate?from=USD&to=XXX"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorDescription").value("Currency code not found: XXX"));
    }

    @Test
    void getRate_shouldReturn400_whenFromCurrencyEmpty() throws Exception {
        // given
        when(exchangeRateService.getExchangeRate("", "EUR"))
                .thenThrow(new IllegalArgumentException("Currency code is required"));

        // then
        mockMvc.perform(get("/api/v1/rate?from=&to=EUR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorDescription").value("Currency code is required"));
    }

    @Test
    void getRate_shouldReturn400_whenToCurrencyEmpty() throws Exception {
        // given
        when(exchangeRateService.getExchangeRate("USD", ""))
                .thenThrow(new IllegalArgumentException("Currency code is required"));

        // then
        mockMvc.perform(get("/api/v1/rate?from=USD&to="))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorDescription").value("Currency code is required"));
    }

    @Test
    void getRate_shouldReturn400_whenBothCurrenciesEmpty() throws Exception {
        // given
        when(exchangeRateService.getExchangeRate("", ""))
                .thenThrow(new IllegalArgumentException("Currency code is required"));

        // then
        mockMvc.perform(get("/api/v1/rate?from=&to="))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorDescription").value("Currency code is required"));
    }

    @Test
    void getRate_shouldReturn400_whenFromCurrencyTooShort() throws Exception {
        // given
        when(exchangeRateService.getExchangeRate("US", "EUR"))
                .thenThrow(new IllegalArgumentException("Invalid currency code: US. Must be 3 letters"));

        // then
        mockMvc.perform(get("/api/v1/rate?from=US&to=EUR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorDescription").value("Invalid currency code: US. Must be 3 letters"));
    }

    @Test
    void getRate_shouldReturn400_whenToCurrencyTooShort() throws Exception {
        // given
        when(exchangeRateService.getExchangeRate("USD", "EU"))
                .thenThrow(new IllegalArgumentException("Invalid currency code: EU. Must be 3 letters"));

        // then
        mockMvc.perform(get("/api/v1/rate?from=USD&to=EU"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorDescription").value("Invalid currency code: EU. Must be 3 letters"));
    }

    @Test
    void getRate_shouldReturn503_whenApiUnavailable() throws Exception {
        // given
        when(exchangeRateService.getExchangeRate("USD", "EUR"))
                .thenThrow(new ExternalServiceException("External currency service unavailable"));

        // then
        mockMvc.perform(get("/api/v1/rate?from=USD&to=EUR"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorDescription").value("External currency service unavailable"));
    }
}