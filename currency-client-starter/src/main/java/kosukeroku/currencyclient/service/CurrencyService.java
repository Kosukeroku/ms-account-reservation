package kosukeroku.currencyclient.service;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import kosukeroku.currencyclient.dto.ExchangeResponse;
import kosukeroku.currencyclient.exception.CurrencyClientException;
import kosukeroku.currencyclient.exception.CurrencyNotFoundException;
import kosukeroku.currencyclient.properties.CurrencyClientProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Slf4j
public class CurrencyService {

    private final CurrencyClientProperties properties;
    private final WebClient webClient;
    private final MeterRegistry meterRegistry;

    @Cacheable(value = "exchangeRates", key = "#p0 + ':' + #p1")
    @Retryable(
            retryFor = {CurrencyClientException.class},
            maxAttemptsExpression = "${app.currency-client.retry-attempts}",
            backoff = @Backoff(
                    delayExpression = "${app.currency-client.retry-delay}",
                    multiplierExpression = "${app.currency-client.retry-multiplier}"
            )
    )
    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {

        validateCurrencyCode(fromCurrency);
        validateCurrencyCode(toCurrency);


        log.info("Getting exchange rate from {} to {}", fromCurrency, toCurrency);

        String url = String.format("/%s/latest/%s", properties.getApiKey(), fromCurrency.toUpperCase());

        try {
            ExchangeResponse response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(ExchangeResponse.class)
                    .block();

            if (response == null || !"success".equals(response.getResult())) {
                throw new CurrencyClientException("API returned an error: " +
                        (response != null ? response.getResult() : "null response"));
            }

            if (response.getConversionRates() == null) {
                throw new CurrencyClientException("Empty conversion rates in API response");
            }

            BigDecimal rate = response.getConversionRates().get(toCurrency.toUpperCase());
            if (rate == null) {
                throw new CurrencyNotFoundException(
                        String.format("Currency '%s' not found", toCurrency.toUpperCase())
                );
            }

            return rate;

        } catch (WebClientResponseException e) {
            log.error("Currency API error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new CurrencyClientException("Currency API request failed with status " + e.getStatusCode());
        } catch (CurrencyNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get exchange rate: {}", e.getMessage());
            throw new CurrencyClientException(
                    "Failed to get exchange rate from " + fromCurrency + " to " + toCurrency
            );
        }
    }

    private void validateCurrencyCode(String currency) {
        if (currency == null || currency.trim().isEmpty()) {
            throw new CurrencyNotFoundException("Currency code is required");
        }
        if (currency.length() != 3) {
            throw new CurrencyNotFoundException("Invalid currency code: " + currency + ". Must be 3 letters");
        }
    }
}