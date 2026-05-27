package kosukeroku.ms_account_reservation.kafka.producer;

import io.micrometer.core.instrument.MeterRegistry;
import kosukeroku.ms_account_reservation.kafka.event.ClientChangedEvent;
import kosukeroku.ms_account_reservation.kafka.event.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "client-events.kafka.producerEnabled", havingValue = "true", matchIfMissing = true)
public class ClientEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    @Value("${client-events.kafka.topic.name}")
    private String topic;

    public void sendClientEvent(UUID clientId, EventType eventType, String eventId) {
        ClientChangedEvent event = new ClientChangedEvent(clientId, eventType, Instant.now(), eventId);
        String key = clientId.toString();

        CompletableFuture<SendResult<String, Object>> future =
                (CompletableFuture<SendResult<String, Object>>) kafkaTemplate.send(topic, key, event);

        future.thenApply(result -> {
            log.info("Successfully delivered message to topic [{}]. Offset: {}, Partition: {}, Key: {}.",
                    topic, result.getRecordMetadata().offset(), result.getRecordMetadata().partition(), key);
            meterRegistry.counter("client-events.kafka.producer.success",
                    "topic", topic,
                    "eventType", eventType.name()
            ).increment();
            return result;
        }).exceptionally(e -> {
            log.error("Unable to send message. Key: {}, error: {}", key, e.getMessage(), e);
            meterRegistry.counter("client-events.kafka.producer.error",
                    "topic", topic,
                    "eventType", eventType.name()
            ).increment();
            return null;
        });
    }
}