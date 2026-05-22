package kosukeroku.ms_account_reservation.kafka.consumer;

import kosukeroku.ms_account_reservation.kafka.event.ClientChangedEvent;
import kosukeroku.ms_account_reservation.kafka.service.IdempotentEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClientEventConsumer {

    private final IdempotentEventService idempotentEventService;

    @Value("${kafka.topic.name}")
    private String topic;

    @KafkaListener(
            topics = "${kafka.topic.name}",
            groupId = "${kafka.consumer.group-id}",
            concurrency = "${kafka.consumer.concurrency:3}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, ClientChangedEvent> record, Acknowledgment ack) {
        String key = record.key();
        ClientChangedEvent event = record.value();

        log.info("Received message from topic {}: key={}, clientId={}, type={}, eventId={}",
                topic, key, event.getClientId(), event.getEventType(), event.getEventId());

        if (idempotentEventService.isProcessed(event.getEventId())) {
            log.info("Duplicate event detected, skipping: eventId={}, key={}", event.getEventId(), key);
            ack.acknowledge();
            return;
        }

        idempotentEventService.saveProcessedEvent(
                event.getEventId(),
                event.getClientId(),
                event.getEventType().name()
        );

        log.info("Processed event successfully: eventId={}, key={}", event.getEventId(), key);

        ack.acknowledge();
    }
}