package kosukeroku.ms_account_reservation.kafka.consumer;

import kosukeroku.ms_account_reservation.kafka.event.ClientChangedEvent;
import kosukeroku.ms_account_reservation.kafka.service.IdempotentEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "client-events.kafka.consumerEnabled", havingValue = "true", matchIfMissing = true)
public class ClientEventConsumer {

    private final IdempotentEventService idempotentEventService;

    @KafkaListener(
            topics = "${client-events.kafka.topic.name}",
            groupId = "${client-events.kafka.consumer.group-id}",
            concurrency = "${client-events.kafka.listener.concurrency:3}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(@Payload ClientChangedEvent event,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String receivedTopic,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset,
                        @Header(KafkaHeaders.RECEIVED_KEY) String key,
                        Acknowledgment ack) {

        log.info("Received message from topic [{}] partition [{}] offset [{}] with key [{}]: clientId={}, type={}, eventId={}",
                receivedTopic, partition, offset, key, event.getClientId(), event.getEventType(), event.getEventId());

        if (idempotentEventService.isProcessed(event.getEventId())) {
            log.info("Duplicate event detected, skipping: eventId={}", event.getEventId());
            ack.acknowledge();
            return;
        }

        idempotentEventService.saveProcessedEvent(
                event.getEventId(),
                event.getClientId(),
                event.getEventType().name()
        );

        log.info("Processed event successfully: eventId={}", event.getEventId());
        ack.acknowledge();
    }
}