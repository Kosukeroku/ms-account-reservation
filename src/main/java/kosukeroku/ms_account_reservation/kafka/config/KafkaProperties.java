package kosukeroku.ms_account_reservation.kafka.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "kafka")
public class KafkaProperties {
    private String bootstrapServers;
    private boolean enabled;
    private Topic topic;
    private Consumer consumer;
    private Producer producer;

    @Data
    public static class Topic {
        private String name;
        private int partitions;
        private short replicationFactor;
        private boolean enabled;
    }

    @Data
    public static class Consumer {
        private boolean enabled;
        private String groupId;
        private int concurrency;
        private int maxPollRecords;
    }

    @Data
    public static class Producer {
        private boolean enabled;
        private String acks;
        private int retries;
    }

}