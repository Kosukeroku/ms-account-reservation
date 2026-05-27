package kosukeroku.ms_account_reservation.kafka.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "client-events.kafka.consumerEnabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private final ClientEventsKafkaProperties properties;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>(properties.buildConsumerProperties());

        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        Map<String, String> consumerProps = properties.getConsumer().getProperties();
        if (consumerProps != null && consumerProps.containsKey("spring.json.type.mapping")) {
            config.put(JsonDeserializer.TYPE_MAPPINGS, consumerProps.get("spring.json.type.mapping"));
        }

        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(properties.getListener().getConcurrency());

        factory.getContainerProperties().setAckMode(properties.getListener().getAckMode());

        return factory;
    }

    @Bean
    public AdminClient adminClient() {
        Map<String, Object> config = new HashMap<>();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        return AdminClient.create(config);
    }
}