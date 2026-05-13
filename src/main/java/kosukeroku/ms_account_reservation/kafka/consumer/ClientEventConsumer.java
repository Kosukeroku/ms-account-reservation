package kosukeroku.ms_account_reservation.kafka.consumer;

import kosukeroku.ms_account_reservation.kafka.event.ClientChangedEvent;
import kosukeroku.ms_account_reservation.model.IdempotentEvent;
import kosukeroku.ms_account_reservation.kafka.repository.IdempotentEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClientEventConsumer {

    private final IdempotentEventRepository idempotentEventRepository;

    @KafkaListener(
            topics = "${kafka.topic.name}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    @Transactional
    public void consume(ClientChangedEvent event, Acknowledgment ack) {
        log.info("Received event: clientId={}, type={}, eventId={}",
                event.getClientId(), event.getEventType(), event.getEventId());

        if (idempotentEventRepository.existsByEventId(event.getEventId())) {
            log.info("Duplicate event detected, skipping: eventId={}", event.getEventId());
            ack.acknowledge();
            return;
        }

        IdempotentEvent record = new IdempotentEvent();
        record.setEventId(event.getEventId());
        record.setClientId(event.getClientId());
        record.setEventType(event.getEventType().name());
        record.setProcessedAt(LocalDateTime.now());
        idempotentEventRepository.save(record);

        log.info("Processed event successfully: eventId={}", event.getEventId());

        ack.acknowledge();
    }
}