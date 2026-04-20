package kosukeroku.currencyclient.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.currency-client")
public class CurrencyClientProperties {
    private String baseUrl = "https://v6.exchangerate-api.com/v6";
    private String apiKey;
    private int connectTimeout = 5000;
    private int readTimeout = 5000;
    private int retryAttempts = 3;
    private long retryDelay = 1000;
    private double retryMultiplier = 2.0;
}