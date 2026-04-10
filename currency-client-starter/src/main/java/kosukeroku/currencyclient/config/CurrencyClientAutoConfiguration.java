package kosukeroku.currencyclient.config;

import io.netty.channel.ChannelOption;
import kosukeroku.currencyclient.properties.CurrencyClientProperties;
import kosukeroku.currencyclient.service.CurrencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(CurrencyClientProperties.class)
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
    public CurrencyService currencyService(WebClient currencyWebClient) {
        return new CurrencyService(properties, currencyWebClient);
    }
}