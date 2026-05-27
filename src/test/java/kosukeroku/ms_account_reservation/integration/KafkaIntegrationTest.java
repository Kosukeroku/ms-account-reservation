package kosukeroku.ms_account_reservation.integration;

import kosukeroku.ms_account_reservation.kafka.event.ClientChangedEvent;
import kosukeroku.ms_account_reservation.kafka.event.EventType;
import kosukeroku.ms_account_reservation.model.IdempotentEvent;
import kosukeroku.ms_account_reservation.kafka.repository.IdempotentEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class KafkaIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private IdempotentEventRepository idempotentEventRepository;

    @Value("${client-events.kafka.topic.name}")
    private String topic;

    @BeforeEach
    void setUp() throws InterruptedException {
        idempotentEventRepository.deleteAll();
        Thread.sleep(2000);
    }

    private ClientChangedEvent createEvent(EventType eventType) {
        String eventId = UUID.randomUUID().toString();
        UUID clientId = UUID.randomUUID();
        return new ClientChangedEvent(clientId, eventType, Instant.now(), eventId);
    }

    @Test
    void shouldProcessEventOnlyOnce() throws Exception {
        // given
        ClientChangedEvent event = createEvent(EventType.CREATED);

        // when
        kafkaTemplate.send(topic, event.getClientId().toString(), event).get(10, TimeUnit.SECONDS);

        // then
        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(idempotentEventRepository.existsByEventId(event.getEventId())).isTrue();
                });
    }

    @Test
    void shouldHandleDuplicateEventIdempotently() throws Exception {
        // given
        ClientChangedEvent event = createEvent(EventType.CREATED);

        // when
        kafkaTemplate.send(topic, event.getClientId().toString(), event).get(10, TimeUnit.SECONDS);

        // then
        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(idempotentEventRepository.existsByEventId(event.getEventId())).isTrue();
                });

        // when
        kafkaTemplate.send(topic, event.getClientId().toString(), event).get(10, TimeUnit.SECONDS);

        // then
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    long count = idempotentEventRepository.findAll().stream()
                            .filter(e -> e.getEventId().equals(event.getEventId()))
                            .count();
                    assertThat(count).isEqualTo(1);
                });
    }

    @Test
    void shouldStoreEventDetailsInDatabase() throws Exception {
        // given
        ClientChangedEvent event = createEvent(EventType.CREATED);

        // when
        kafkaTemplate.send(topic, event.getClientId().toString(), event).get(10, TimeUnit.SECONDS);

        // then
        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    IdempotentEvent record = idempotentEventRepository.findAll().stream()
                            .filter(e -> e.getEventId().equals(event.getEventId()))
                            .findFirst()
                            .orElse(null);

                    assertThat(record).isNotNull();
                    assertThat(record.getClientId()).isEqualTo(event.getClientId());
                    assertThat(record.getEventType()).isEqualTo(event.getEventType().name());
                    assertThat(record.getProcessedAt()).isNotNull();
                });
    }
}