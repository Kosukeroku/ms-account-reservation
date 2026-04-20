package kosukeroku.ms_account_reservation.service;

import kosukeroku.currencyclient.exception.CurrencyClientException;
import kosukeroku.currencyclient.exception.CurrencyNotFoundException;
import kosukeroku.currencyclient.service.CurrencyService;
import kosukeroku.ms_account_reservation.exception.ExternalServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {

    private final CurrencyService currencyService;

    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        try {
            return currencyService.getExchangeRate(fromCurrency, toCurrency);
        } catch (CurrencyNotFoundException e) {
            log.warn("Currency error: {}", e.getMessage());
            throw new IllegalArgumentException(e.getMessage());
        } catch (CurrencyClientException e) {
            log.error("Currency API error: {}", e.getMessage());
            throw new ExternalServiceException("External currency service unavailable");
        }
    }
}