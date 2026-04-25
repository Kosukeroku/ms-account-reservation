package kosukeroku.currencyclient.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.netty.channel.ChannelOption;
import kosukeroku.currencyclient.health.CurrencyApiHealthIndicator;
import kosukeroku.currencyclient.properties.CurrencyClientProperties;
import kosukeroku.currencyclient.service.CurrencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(CurrencyClientProperties.class)
@ConditionalOnProperty(name = "currency-client-starter.enabled", havingValue = "true", matchIfMissing = true)
@EnableRetry
public class CurrencyClientAutoConfiguration {

    private final CurrencyClientProperties properties;

    @Bean
    public WebClient currencyWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectTimeout())
                .responseTimeout(Duration.ofMillis(properties.getReadTimeout()));

        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "currency-client-starter.enabled", havingValue = "true", matchIfMissing = true)
    public CurrencyService currencyService(WebClient currencyWebClient, MeterRegistry meterRegistry) {
        return new CurrencyService(properties, currencyWebClient, meterRegistry);
    }

    @Bean
    @ConditionalOnProperty(name = "app.currency-client.health-indicator.enabled", havingValue = "true", matchIfMissing = false)
    public HealthIndicator currencyApiHealthIndicator(WebClient currencyWebClient, CurrencyClientProperties properties) {
        return new CurrencyApiHealthIndicator(currencyWebClient, properties);
    }
}