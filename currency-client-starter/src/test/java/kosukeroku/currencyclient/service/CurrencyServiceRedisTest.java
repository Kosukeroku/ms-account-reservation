package kosukeroku.currencyclient.service;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DataRedisTest
@Testcontainers
class CurrencyServiceRedisTest {

    @Container
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    void testCachePutAndGet() {
        // given
        String key = "USD:EUR";
        String value = "0.8511";

        // when
        redisTemplate.opsForValue().set(key, value);
        String result = redisTemplate.opsForValue().get(key);

        // then
        assertThat(result).isEqualTo(value);
    }

    @Test
    void testCacheGetNonExistentKey() {
        // given
        String key = "non:existent";

        // when
        String result = redisTemplate.opsForValue().get(key);

        // then
        assertThat(result).isNull();
    }

    @Test
    void testCacheDelete() {
        // given
        String key = "USD:GBP";
        String value = "0.7402";

        // when
        redisTemplate.opsForValue().set(key, value);
        redisTemplate.delete(key);
        String result = redisTemplate.opsForValue().get(key);

        // then
        assertThat(result).isNull();
    }

    @Test
    void testCacheWithTTL() {
        // given
        String key = "USD:RUB";
        String value = "75.0245";

        // when
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(1));

        // then
        assertThat(redisTemplate.opsForValue().get(key)).isEqualTo(value);

        // when
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // then
        assertThat(redisTemplate.opsForValue().get(key)).isNull();
    }
}