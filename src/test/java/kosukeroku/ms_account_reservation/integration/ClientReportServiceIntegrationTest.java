package kosukeroku.ms_account_reservation.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.testcontainers.RedisContainer;
import kosukeroku.ms_account_reservation.dto.ClientCreateRequest;
import kosukeroku.ms_account_reservation.repository.ClientRepository;
import kosukeroku.ms_account_reservation.service.ExchangeRateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class ClientReportServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.liquibase.enabled", () -> "false");
        registry.add("app.async.timeout-seconds", () -> "5");
    }

    @MockitoBean
    private ExchangeRateService exchangeRateService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClientRepository clientRepository;

    private UUID createdClientId;

    @BeforeEach
    void setUp() throws Exception {
        clientRepository.deleteAll();

        ClientCreateRequest request = new ClientCreateRequest();
        request.setMdmId(1234567890L);
        request.setFirstName("Иван");
        request.setLastName("Петров");
        request.setMiddleName("Сергеевич");
        request.setCitizenship("РФ");
        request.setClientType("INDIVIDUAL");
        request.setDocumentNumber("123456");
        request.setDocumentSeries("1234");
        request.setDocumentType("PASSPORT");

        String response = mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        createdClientId = UUID.fromString(
                objectMapper.readTree(response).get("id").asText()
        );
    }

    @AfterEach
    void tearDown() {
        clientRepository.deleteAll();
    }

    @Test
    void getClientReport_shouldReturnFullReport_whenAllDataAvailable() throws Exception {
        // given
        when(exchangeRateService.getExchangeRate("USD", "RUB"))
                .thenReturn(new BigDecimal("75.0"));
        when(exchangeRateService.getExchangeRate("EUR", "RUB"))
                .thenReturn(new BigDecimal("85.0"));

        // then
        mockMvc.perform(get("/api/v1/clients/{id}/report", createdClientId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(createdClientId.toString()))
                .andExpect(jsonPath("$.firstName").value("Иван"))
                .andExpect(jsonPath("$.lastName").value("Петров"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.exchangeRates['USD/RUB']").value(75.0))
                .andExpect(jsonPath("$.exchangeRates['EUR/RUB']").value(85.0));
    }

    @Test
    void getClientReport_shouldReturnPartialReport_whenUsdRateFails() throws Exception {
        // given
        when(exchangeRateService.getExchangeRate("USD", "RUB"))
                .thenThrow(new RuntimeException("API error"));
        when(exchangeRateService.getExchangeRate("EUR", "RUB"))
                .thenReturn(new BigDecimal("85.0"));

        // then
        mockMvc.perform(get("/api/v1/clients/{id}/report", createdClientId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exchangeRates['USD/RUB']").doesNotExist())
                .andExpect(jsonPath("$.exchangeRates['EUR/RUB']").value(85.0));
    }

    @Test
    void getClientReport_shouldReturnPartialReport_whenEurRateFails() throws Exception {
        // given
        when(exchangeRateService.getExchangeRate("USD", "RUB"))
                .thenReturn(new BigDecimal("75.0"));
        when(exchangeRateService.getExchangeRate("EUR", "RUB"))
                .thenThrow(new RuntimeException("API error"));

        // then
        mockMvc.perform(get("/api/v1/clients/{id}/report", createdClientId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exchangeRates['USD/RUB']").value(75.0))
                .andExpect(jsonPath("$.exchangeRates['EUR/RUB']").doesNotExist());
    }

    @Test
    void getClientReport_shouldReturnEmptyRates_whenAllRatesFail() throws Exception {
        // given
        when(exchangeRateService.getExchangeRate("USD", "RUB"))
                .thenThrow(new RuntimeException("API error"));
        when(exchangeRateService.getExchangeRate("EUR", "RUB"))
                .thenThrow(new RuntimeException("API error"));

        // then
        mockMvc.perform(get("/api/v1/clients/{id}/report", createdClientId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exchangeRates").isEmpty());
    }

    @Test
    void getClientReport_shouldReturn404_whenClientNotFound() throws Exception {
        // given
        UUID nonExistentId = UUID.randomUUID();

        // then
        mockMvc.perform(get("/api/v1/clients/{id}/report", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CLIENT_NOT_FOUND"));
    }

    @Test
    void getClientReport_shouldExecuteAsynchronously() throws Exception {
        // given
        when(exchangeRateService.getExchangeRate("USD", "RUB")).thenAnswer(invocation -> {
            Thread.sleep(300);
            return new BigDecimal("75.0");
        });

        when(exchangeRateService.getExchangeRate("EUR", "RUB")).thenAnswer(invocation -> {
            Thread.sleep(300);
            return new BigDecimal("85.0");
        });

        long startTime = System.currentTimeMillis();

        // when
        mockMvc.perform(get("/api/v1/clients/{id}/report", createdClientId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        long duration = System.currentTimeMillis() - startTime;

        // then
        assertThat(duration).isLessThan(500);
    }
}