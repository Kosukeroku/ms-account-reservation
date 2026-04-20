package kosukeroku.ms_account_reservation.service;

import kosukeroku.currencyclient.exception.CurrencyClientException;
import kosukeroku.currencyclient.exception.CurrencyNotFoundException;
import kosukeroku.currencyclient.service.CurrencyService;
import kosukeroku.ms_account_reservation.exception.ExternalServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private CurrencyService currencyService;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    @Test
    void getExchangeRate_shouldReturnRate_whenCurrencyServiceReturnsRate() {
        // given
        BigDecimal expectedRate = new BigDecimal("0.86");
        when(currencyService.getExchangeRate("USD", "EUR")).thenReturn(expectedRate);

        // when
        BigDecimal result = exchangeRateService.getExchangeRate("USD", "EUR");

        // then
        assertThat(result).isEqualTo(expectedRate);
    }

    @Test
    void getExchangeRate_shouldThrowIllegalArgumentException_whenToCurrencyNotFound() {
        // given
        when(currencyService.getExchangeRate("USD", "XXX"))
                .thenThrow(new CurrencyNotFoundException("Currency 'XXX' not found"));

        // then
        assertThatThrownBy(() -> exchangeRateService.getExchangeRate("USD", "XXX"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency 'XXX' not found");
    }

    @Test
    void getExchangeRate_shouldThrowIllegalArgumentException_whenFromCurrencyNotFound() {
        // given
        when(currencyService.getExchangeRate("XXX", "EUR"))
                .thenThrow(new CurrencyNotFoundException("Currency 'XXX' not found"));

        // then
        assertThatThrownBy(() -> exchangeRateService.getExchangeRate("XXX", "EUR"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency 'XXX' not found");
    }

    @Test
    void getExchangeRate_shouldThrowIllegalArgumentException_whenFromCurrencyInvalidFormat() {
        // given
        when(currencyService.getExchangeRate("US", "EUR"))
                .thenThrow(new CurrencyNotFoundException("Invalid currency code: US. Must be 3 letters"));

        // then
        assertThatThrownBy(() -> exchangeRateService.getExchangeRate("US", "EUR"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid currency code: US. Must be 3 letters");
    }

    @Test
    void getExchangeRate_shouldThrowIllegalArgumentException_whenToCurrencyInvalidFormat() {
        // given
        when(currencyService.getExchangeRate("USD", "EU"))
                .thenThrow(new CurrencyNotFoundException("Invalid currency code: EU. Must be 3 letters"));

        // then
        assertThatThrownBy(() -> exchangeRateService.getExchangeRate("USD", "EU"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid currency code: EU. Must be 3 letters");
    }

    @Test
    void getExchangeRate_shouldThrowExternalServiceException_whenCurrencyApiFails() {
        // given
        when(currencyService.getExchangeRate("USD", "EUR"))
                .thenThrow(new CurrencyClientException("Currency API request failed"));

        // then
        assertThatThrownBy(() -> exchangeRateService.getExchangeRate("USD", "EUR"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("External currency service unavailable");
    }

}