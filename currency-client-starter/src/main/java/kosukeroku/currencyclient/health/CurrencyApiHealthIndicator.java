package kosukeroku.currencyclient.health;

import kosukeroku.currencyclient.properties.CurrencyClientProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        name = "app.currency-client.health-indicator.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class CurrencyApiHealthIndicator implements HealthIndicator {

    private final WebClient webClient;
    private final CurrencyClientProperties properties;

    @Override
    public Health health() {
        String url = String.format("/%s/latest/USD", properties.getApiKey());

        try {
            webClient.get()
                    .uri(url)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            return Health.up()
                    .withDetail("api", "available")
                    .build();

        } catch (WebClientResponseException e) {
            int statusCode = e.getStatusCode().value();
            log.warn("Currency API health check failed with status: {}", statusCode);
            return Health.down()
                    .withDetail("api", "unavailable")
                    .withDetail("statusCode", statusCode)
                    .withDetail("reason", e.getMessage())
                    .build();

        } catch (Exception e) {
            log.error("Unexpected error during health check", e);
            return Health.down(e)
                    .withDetail("api", "error")
                    .withDetail("reason", e.getMessage())
                    .build();
        }
    }
}