package kosukeroku.currencyclient.integration;

import kosukeroku.currencyclient.config.CurrencyClientAutoConfiguration;
import kosukeroku.currencyclient.dto.ExchangeResponse;
import kosukeroku.currencyclient.exception.CurrencyClientException;
import kosukeroku.currencyclient.exception.CurrencyNotFoundException;
import kosukeroku.currencyclient.properties.CurrencyClientProperties;
import kosukeroku.currencyclient.service.CurrencyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {CurrencyClientAutoConfiguration.class})
class CurrencyServiceRetryIntegrationTest {

    @Autowired
    private CurrencyService currencyService;

    @MockitoBean
    private WebClient webClient;

    @MockitoBean
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @MockitoBean
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @MockitoBean
    private WebClient.ResponseSpec responseSpec;

    private ExchangeResponse createSuccessResponse() {
        ExchangeResponse response = new ExchangeResponse();
        response.setResult("success");
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("EUR", new BigDecimal("0.86"));
        response.setConversionRates(rates);
        return response;
    }

    private void setupWebClientMock() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void retry_shouldSucceedOnSecondAttempt_whenFirstFails() {
        // given
        setupWebClientMock();
        ExchangeResponse response = createSuccessResponse();

        when(responseSpec.bodyToMono(ExchangeResponse.class))
                .thenReturn(Mono.error(new RuntimeException("Connection refused")))
                .thenReturn(Mono.just(response));

        // when
        BigDecimal result = currencyService.getExchangeRate("USD", "EUR");

        // then
        assertThat(result).isEqualTo(new BigDecimal("0.86"));
    }

    @Test
    void retry_shouldSucceedOnThirdAttempt_whenFirstTwoFail() {
        // given
        setupWebClientMock();
        ExchangeResponse response = createSuccessResponse();

        when(responseSpec.bodyToMono(ExchangeResponse.class))
                .thenReturn(Mono.error(new RuntimeException("Connection refused")))
                .thenReturn(Mono.error(new RuntimeException("Timeout")))
                .thenReturn(Mono.just(response));

        // when
        BigDecimal result = currencyService.getExchangeRate("USD", "EUR");

        // then
        assertThat(result).isEqualTo(new BigDecimal("0.86"));
    }

    @Test
    void retry_shouldThrowException_whenAllAttemptsFail() {
        // given
        setupWebClientMock();

        when(responseSpec.bodyToMono(ExchangeResponse.class))
                .thenReturn(Mono.error(new RuntimeException("Connection refused")));

        // then
        assertThatThrownBy(() -> currencyService.getExchangeRate("USD", "EUR"))
                .isInstanceOf(CurrencyClientException.class);
    }

    @Test
    void retry_shouldNotRetry_whenCurrencyNotFoundException() {
        // given
        setupWebClientMock();
        ExchangeResponse response = new ExchangeResponse();
        response.setResult("success");
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("USD", new BigDecimal("1"));
        response.setConversionRates(rates);

        when(responseSpec.bodyToMono(ExchangeResponse.class)).thenReturn(Mono.just(response));

        // then
        assertThatThrownBy(() -> currencyService.getExchangeRate("USD", "EUR"))
                .isInstanceOf(CurrencyNotFoundException.class);
    }
}