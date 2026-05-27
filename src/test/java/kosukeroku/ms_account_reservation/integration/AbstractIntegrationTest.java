package kosukeroku.ms_account_reservation.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    protected static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.4.0")
    );

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.liquibase.enabled", () -> "false");

        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);

        registry.add("kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("kafka.enabled", () -> "true");
        registry.add("kafka.consumer.enabled", () -> "true");
        registry.add("kafka.consumer.group-id", () -> "test-group-" + UUID.randomUUID());
        registry.add("kafka.consumer.concurrency", () -> "1");
        registry.add("kafka.consumer.max-poll-records", () -> "500");

        registry.add("kafka.producer.enabled", () -> "true");
        registry.add("kafka.producer.acks", () -> "all");
        registry.add("kafka.producer.retries", () -> "3");

        registry.add("kafka.topic.enabled", () -> "true");
        registry.add("kafka.topic.name", () -> "client-events");
        registry.add("kafka.topic.partitions", () -> "1");
        registry.add("kafka.topic.replication-factor", () -> "1");

        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.consumer.enable-auto-commit", () -> "false");
        registry.add("spring.kafka.listener.ack-mode", () -> "manual");
        registry.add("spring.kafka.listener.retry.max-attempts", () -> "1");
        registry.add("spring.kafka.admin.auto-create", () -> "true");
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected static final int DEFAULT_PAGE = 0;
    protected static final int DEFAULT_SIZE = 20;
}