package kosukeroku.ms_account_reservation.kafka.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Data
@Component
@Primary
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties(prefix = "client-events.kafka")
public class ClientEventsKafkaProperties extends KafkaProperties {

    private Topic topic = new Topic();
    private boolean producerEnabled = true;
    private boolean consumerEnabled = true;

    @Data
    public static class Topic {
        private String name;
    }
}