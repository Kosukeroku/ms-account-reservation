package kosukeroku.ms_account_reservation.kafka.producer;

import io.micrometer.core.instrument.MeterRegistry;
import kosukeroku.ms_account_reservation.kafka.event.ClientChangedEvent;
import kosukeroku.ms_account_reservation.kafka.event.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClientEventProducer {

    private final KafkaTemplate<String, ClientChangedEvent> kafkaTemplate;

    @Value("${kafka.topic.name}")
    private String topic;

    private final MeterRegistry meterRegistry;

    public void sendClientEvent(UUID clientId, EventType eventType, String eventId) {
        ClientChangedEvent event = new ClientChangedEvent(
                clientId,
                eventType,
                Instant.now(),
                eventId
        );

        CompletableFuture<SendResult<String, ClientChangedEvent>> future =
                kafkaTemplate.send(topic, clientId.toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send event for client {}: {}", clientId, ex.getMessage());
            } else {
                meterRegistry.counter("kafka.event.published",
                        "topic", topic,
                        "eventType", eventType.name()
                ).increment();

                log.info("Event sent for client {}: offset={}, partition={}",
                        clientId,
                        result.getRecordMetadata().offset(),
                        result.getRecordMetadata().partition());
            }
        });
    }
}