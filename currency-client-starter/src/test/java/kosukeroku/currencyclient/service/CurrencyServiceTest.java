package kosukeroku.currencyclient.service;

import kosukeroku.currencyclient.dto.ExchangeResponse;
import kosukeroku.currencyclient.exception.CurrencyClientException;
import kosukeroku.currencyclient.exception.CurrencyNotFoundException;
import kosukeroku.currencyclient.properties.CurrencyClientProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceTest {

    @Mock
    private CurrencyClientProperties properties;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private CurrencyService currencyService;

    @BeforeEach
    void setUp() {
        currencyService = new CurrencyService(properties, webClient);
        lenient().when(properties.getApiKey()).thenReturn("test-api-key");
        lenient().when(properties.getRetryAttempts()).thenReturn(3);
        lenient().when(properties.getRetryDelay()).thenReturn(100L);
    }

    @Test
    void getExchangeRate_shouldReturnRate_whenApiReturnsSuccess() {
        // given
        ExchangeResponse response = new ExchangeResponse();
        response.setResult("success");
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("EUR", new BigDecimal("0.86"));
        response.setConversionRates(rates);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/test-api-key/latest/USD")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ExchangeResponse.class)).thenReturn(Mono.just(response));

        // when
        BigDecimal result = currencyService.getExchangeRate("USD", "EUR");

        // then
        assertThat(result).isEqualTo(new BigDecimal("0.86"));
    }

    @Test
    void getExchangeRate_shouldThrowCurrencyNotFoundException_whenToCurrencyNotFound() {
        // given
        ExchangeResponse response = new ExchangeResponse();
        response.setResult("success");
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("USD", new BigDecimal("1"));
        rates.put("RUB", new BigDecimal("100"));
        response.setConversionRates(rates);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/test-api-key/latest/USD")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ExchangeResponse.class)).thenReturn(Mono.just(response));

        // then
        assertThatThrownBy(() -> currencyService.getExchangeRate("USD", "EUR"))
                .isInstanceOf(CurrencyNotFoundException.class)
                .hasMessageContaining("Currency 'EUR' not found");
    }

    @Test
    void getExchangeRate_shouldThrowCurrencyNotFoundException_whenFromCurrencyEmpty() {
        assertThatThrownBy(() -> currencyService.getExchangeRate("", "EUR"))
                .isInstanceOf(CurrencyNotFoundException.class)
                .hasMessage("Currency code is required");
    }

    @Test
    void getExchangeRate_shouldThrowCurrencyNotFoundException_whenToCurrencyEmpty() {
        assertThatThrownBy(() -> currencyService.getExchangeRate("USD", ""))
                .isInstanceOf(CurrencyNotFoundException.class)
                .hasMessage("Currency code is required");
    }

    @Test
    void getExchangeRate_shouldThrowCurrencyNotFoundException_whenBothCurrenciesEmpty() {
        assertThatThrownBy(() -> currencyService.getExchangeRate("", ""))
                .isInstanceOf(CurrencyNotFoundException.class)
                .hasMessage("Currency code is required");
    }

    @Test
    void getExchangeRate_shouldThrowCurrencyNotFoundException_whenFromCurrencyTooShort() {
        assertThatThrownBy(() -> currencyService.getExchangeRate("US", "EUR"))
                .isInstanceOf(CurrencyNotFoundException.class)
                .hasMessage("Invalid currency code: US. Must be 3 letters");
    }

    @Test
    void getExchangeRate_shouldThrowCurrencyNotFoundException_whenToCurrencyTooShort() {
        assertThatThrownBy(() -> currencyService.getExchangeRate("USD", "EU"))
                .isInstanceOf(CurrencyNotFoundException.class)
                .hasMessage("Invalid currency code: EU. Must be 3 letters");
    }

    @Test
    void getExchangeRate_shouldThrowCurrencyNotFoundException_whenFromCurrencyNull() {
        assertThatThrownBy(() -> currencyService.getExchangeRate(null, "EUR"))
                .isInstanceOf(CurrencyNotFoundException.class)
                .hasMessage("Currency code is required");
    }

    @Test
    void getExchangeRate_shouldThrowCurrencyNotFoundException_whenToCurrencyNull() {
        assertThatThrownBy(() -> currencyService.getExchangeRate("USD", null))
                .isInstanceOf(CurrencyNotFoundException.class)
                .hasMessage("Currency code is required");
    }

    @Test
    void getExchangeRate_shouldThrowCurrencyNotFoundException_whenFromCurrencyInvalidAndToCurrencyValid() {
        assertThatThrownBy(() -> currencyService.getExchangeRate("XX", "EUR"))
                .isInstanceOf(CurrencyNotFoundException.class)
                .hasMessage("Invalid currency code: XX. Must be 3 letters");
    }

    @Test
    void getExchangeRate_shouldThrowCurrencyNotFoundException_whenFromCurrencyValidAndToCurrencyInvalid() {
        assertThatThrownBy(() -> currencyService.getExchangeRate("USD", "XX"))
                .isInstanceOf(CurrencyNotFoundException.class)
                .hasMessage("Invalid currency code: XX. Must be 3 letters");
    }
}