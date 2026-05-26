package kosukeroku.ms_account_reservation.kafka.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "client-events.kafka.producerEnabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class KafkaProducerConfig {

    private final ClientEventsKafkaProperties properties;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>(properties.buildProducerProperties());
        config.put("bootstrap.servers", properties.getBootstrapServers());
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}