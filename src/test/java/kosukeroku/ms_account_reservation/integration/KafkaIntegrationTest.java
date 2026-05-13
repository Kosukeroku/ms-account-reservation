package kosukeroku.ms_account_reservation.integration;

import kosukeroku.ms_account_reservation.kafka.event.ClientChangedEvent;
import kosukeroku.ms_account_reservation.kafka.event.EventType;
import kosukeroku.ms_account_reservation.model.IdempotentEvent;
import kosukeroku.ms_account_reservation.kafka.repository.IdempotentEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers
@SpringBootTest
class KafkaIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.4.0")
    );

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.liquibase.enabled", () -> "false");

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.group-id", () -> "test-group");
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.consumer.enable-auto-commit", () -> "false");
        registry.add("spring.kafka.listener.ack-mode", () -> "manual");

        registry.add("kafka.topic.name", () -> "client-events");
        registry.add("kafka.topic.partitions", () -> "1");
        registry.add("kafka.topic.replication-factor", () -> "1");

        registry.add("spring.kafka.listener.retry.max-attempts", () -> "3");
        registry.add("spring.kafka.listener.retry.initial-interval", () -> "100");
        registry.add("spring.kafka.listener.retry.multiplier", () -> "2.0");
    }

    @Autowired
    private KafkaTemplate<String, ClientChangedEvent> kafkaTemplate;

    @Autowired
    private IdempotentEventRepository idempotentEventRepository;

    @Value("${kafka.topic.name}")
    private String topic;

    @BeforeEach
    void setUp() {
        idempotentEventRepository.deleteAll();
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
        kafkaTemplate.send(topic, event.getClientId().toString(), event).get(5, TimeUnit.SECONDS);

        // then
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(idempotentEventRepository.existsByEventId(event.getEventId())).isTrue();
                });
    }

    @Test
    void shouldHandleDuplicateEventIdempotently() throws Exception {
        // given
        ClientChangedEvent event = createEvent(EventType.CREATED);

        // when
        kafkaTemplate.send(topic, event.getClientId().toString(), event).get(5, TimeUnit.SECONDS);

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(idempotentEventRepository.existsByEventId(event.getEventId())).isTrue();
                });

        kafkaTemplate.send(topic, event.getClientId().toString(), event).get(5, TimeUnit.SECONDS);

        // then
        await().atMost(2, TimeUnit.SECONDS)
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
        kafkaTemplate.send(topic, event.getClientId().toString(), event).get(5, TimeUnit.SECONDS);

        // then
        await().atMost(5, TimeUnit.SECONDS)
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