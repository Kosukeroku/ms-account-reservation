package kosukeroku.ms_account_reservation.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import kosukeroku.ms_account_reservation.dto.ClientCreateRequest;
import kosukeroku.ms_account_reservation.model.Account;
import kosukeroku.ms_account_reservation.model.AccountStatus;
import kosukeroku.ms_account_reservation.model.enums.AccountStatusName;
import kosukeroku.ms_account_reservation.repository.AccountRepository;
import kosukeroku.ms_account_reservation.repository.AccountStatusRepository;
import kosukeroku.ms_account_reservation.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class QueryCountIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.liquibase.enabled", () -> "false");
        registry.add("logging.level.org.hibernate.SQL", () -> "DEBUG");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountStatusRepository accountStatusRepository;

    private UUID createdClientId;

    @BeforeEach
    void setUp() throws Exception {
        accountRepository.deleteAll();
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

        createTestAccounts(createdClientId);
    }

    @Test
    void getClientById_shouldExecuteOneQuery(CapturedOutput output) throws Exception {
        // given
        String logsBefore = output.toString();

        // when
        mockMvc.perform(get("/api/v1/clients/{clientId}", createdClientId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        String logsAfter = output.toString();

        String newLogs = logsAfter.substring(logsBefore.length());

        long selectCount = newLogs.lines()
                .filter(line -> line.contains("DEBUG") && line.contains("select"))
                .count();

        // then
        assertThat(selectCount).isEqualTo(1);
    }

    @Test
    void searchClients_shouldExecuteTwoQueries(CapturedOutput output) throws Exception {
        // given
        String logsBefore = output.toString();

        // when
        mockMvc.perform(get("/api/v1/clients")
                        .param("lastName", "Петров")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        String logsAfter = output.toString();
        String newLogs = logsAfter.substring(logsBefore.length());

        long selectCount = newLogs.lines()
                .filter(line -> line.contains("DEBUG") && line.contains("select"))
                .count();

        // then
        assertThat(selectCount).isEqualTo(2);
    }

    private void createTestAccounts(UUID clientId) {
        AccountStatus newStatus = accountStatusRepository.findByName(AccountStatusName.NEW)
                .orElseGet(() -> accountStatusRepository.save(new AccountStatus(AccountStatusName.NEW)));

        AccountStatus createdStatus = accountStatusRepository.findByName(AccountStatusName.CREATED)
                .orElseGet(() -> accountStatusRepository.save(new AccountStatus(AccountStatusName.CREATED)));

        var client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        Account account1 = new Account();
        account1.setClient(client);
        account1.setStatus(newStatus);
        account1.setCurrencyCode("USD");
        account1.setBalance(new BigDecimal("1000.00"));
        account1.setAccountNumber("ACC001");
        account1.setAccountType("CHECKING");
        accountRepository.save(account1);

        Account account2 = new Account();
        account2.setClient(client);
        account2.setStatus(createdStatus);
        account2.setCurrencyCode("EUR");
        account2.setBalance(new BigDecimal("5000.00"));
        account2.setAccountNumber("ACC002");
        account2.setAccountType("SAVINGS");
        accountRepository.save(account2);
    }
}