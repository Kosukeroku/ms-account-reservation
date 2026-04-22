package kosukeroku.currencyclient.health;

import kosukeroku.currencyclient.exception.CurrencyClientException;
import kosukeroku.currencyclient.exception.CurrencyNotFoundException;
import kosukeroku.currencyclient.service.CurrencyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyApiHealthIndicatorTest {

    @Mock
    private CurrencyService currencyService;

    @InjectMocks
    private CurrencyApiHealthIndicator healthIndicator;

    @Test
    void health_shouldReturnUp_whenApiIsAvailable() {
        // given
        when(currencyService.getExchangeRate("USD", "EUR")).thenReturn(null);

        // when
        Health health = healthIndicator.health();

        // then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails().get("api")).isEqualTo("available");
    }

    @Test
    void health_shouldReturnDown_whenApiThrowsCurrencyClientException() {
        // given
        when(currencyService.getExchangeRate("USD", "EUR"))
                .thenThrow(new CurrencyClientException("Currency API request failed with status 500"));

        // when
        Health health = healthIndicator.health();

        // then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails().get("api")).isEqualTo("unavailable");
        assertThat(health.getDetails().get("reason")).isEqualTo("Currency API request failed with status 500");
    }

    @Test
    void health_shouldReturnDown_whenApiThrowsUnexpectedException() {
        // given
        when(currencyService.getExchangeRate("USD", "EUR"))
                .thenThrow(new RuntimeException("Connection refused"));

        // when
        Health health = healthIndicator.health();

        // then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails().get("api")).isEqualTo("error");
    }
}