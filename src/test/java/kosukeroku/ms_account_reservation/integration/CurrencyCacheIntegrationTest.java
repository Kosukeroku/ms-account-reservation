package kosukeroku.ms_account_reservation.integration;

import com.redis.testcontainers.RedisContainer;
import kosukeroku.ms_account_reservation.service.ExchangeRateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class CurrencyCacheIntegrationTest {

    @Container
    static RedisContainer redis = new RedisContainer(
            DockerImageName.parse("redis:7-alpine")
    ).withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void cache_shouldStoreAndRetrieveValue() {
        // given
        cacheManager.getCache("exchangeRates").clear();

        // when
        BigDecimal first = exchangeRateService.getExchangeRate("USD", "EUR");
        BigDecimal second = exchangeRateService.getExchangeRate("USD", "EUR");

        // then
        assertThat(first).isEqualTo(second);

        Object cached = cacheManager.getCache("exchangeRates").get("USD:EUR").get();
        assertThat(cached).isEqualTo(first);
    }

    @Test
    void cache_shouldReturnDifferentValuesForDifferentKeys() {
        // given
        cacheManager.getCache("exchangeRates").clear();

        // when
        BigDecimal usdToEur = exchangeRateService.getExchangeRate("USD", "EUR");
        BigDecimal usdToGbp = exchangeRateService.getExchangeRate("USD", "GBP");

        // then
        assertThat(usdToEur).isNotEqualTo(usdToGbp);

        assertThat(cacheManager.getCache("exchangeRates").get("USD:EUR")).isNotNull();
        assertThat(cacheManager.getCache("exchangeRates").get("USD:GBP")).isNotNull();
    }
}