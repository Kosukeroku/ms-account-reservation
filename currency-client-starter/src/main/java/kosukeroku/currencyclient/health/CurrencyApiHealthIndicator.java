package kosukeroku.currencyclient.health;

import kosukeroku.currencyclient.exception.CurrencyClientException;
import kosukeroku.currencyclient.service.CurrencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        name = "app.currency-client.health-indicator.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class CurrencyApiHealthIndicator implements HealthIndicator {

    private final CurrencyService currencyService;

    @Override
    public Health health() {
        try {
            currencyService.getExchangeRate("USD", "EUR");
            return Health.up().withDetail("api", "available").build();
        } catch (CurrencyClientException e) {
            log.warn("Currency API health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("api", "unavailable")
                    .withDetail("reason", e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("Unexpected error during health check", e);
            return Health.down(e).withDetail("api", "error").build();
        }
    }
}