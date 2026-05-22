package kosukeroku.ms_account_reservation.kafka.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaTopicConfig {

    private final KafkaProperties kafkaProperties;

    @Bean
    @ConditionalOnProperty(name = "kafka.topic.enabled", havingValue = "true", matchIfMissing = true)
    public NewTopic clientEventsTopic() {
        return new NewTopic(
                kafkaProperties.getTopic().getName(),
                kafkaProperties.getTopic().getPartitions(),
                kafkaProperties.getTopic().getReplicationFactor()
        );
    }
}