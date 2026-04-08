package kosukeroku.ms_account_reservation.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import kosukeroku.ms_account_reservation.dto.ClientCreateRequest;
import kosukeroku.ms_account_reservation.dto.ClientUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClientIntegrationTest extends AbstractIntegrationTest {

    private ClientCreateRequest createRequest;
    private ClientUpdateRequest updateRequest;
    private UUID createdClientId;

    @BeforeEach
    void setUp() {
        createRequest = new ClientCreateRequest();
        createRequest.setMdmId(1234567890L);
        createRequest.setFirstName("Иван");
        createRequest.setLastName("Петров");
        createRequest.setMiddleName("Сергеевич");
        createRequest.setCitizenship("РФ");
        createRequest.setClientType("INDIVIDUAL");
        createRequest.setDocumentNumber("123456");
        createRequest.setDocumentSeries("1234");
        createRequest.setDocumentType("PASSPORT");

        updateRequest = new ClientUpdateRequest();
        updateRequest.setFirstName("Петр");
        updateRequest.setLastName("Иванов");
        updateRequest.setMiddleName("Александрович");
    }

    // POST /clients
    @Test
    void createClient_shouldReturn201AndClient_whenValidRequest() throws Exception {
        // given
        String requestBody = objectMapper.writeValueAsString(createRequest);

        // when
        String response = mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mdmId").value(1234567890L))
                .andExpect(jsonPath("$.firstName").value("Иван"))
                .andExpect(jsonPath("$.lastName").value("Петров"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // then
        createdClientId = UUID.fromString(
                objectMapper.readTree(response).get("id").asText()
        );
    }

    @Test
    void createClient_shouldReturn409_whenMdmIdAlreadyExists() throws Exception {
        // given
        createClient_shouldReturn201AndClient_whenValidRequest();
        String requestBody = objectMapper.writeValueAsString(createRequest);

        // then
        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CLIENT_ALREADY_EXISTS"));
    }

    @Test
    void createClient_shouldReturn400_whenFirstNameIsInvalid() throws Exception {
        // given
        createRequest.setFirstName("");
        String requestBody = objectMapper.writeValueAsString(createRequest);

        // then
        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void createClient_shouldReturn400_whenMdmIdIsInvalid() throws Exception {
        // given
        createRequest.setMdmId(null);
        String requestBody = objectMapper.writeValueAsString(createRequest);

        // then
        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    // GET /clients/{clientId}
    @Test
    void getClientById_shouldReturn200AndClient_whenClientExists() throws Exception {
        // given
        createClient_shouldReturn201AndClient_whenValidRequest();

        // then
        mockMvc.perform(get("/api/v1/clients/{clientId}", createdClientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdClientId.toString()))
                .andExpect(jsonPath("$.mdmId").value(1234567890L));
    }

    @Test
    void getClientById_shouldReturn404_whenClientDoesNotExist() throws Exception {
        // given
        UUID nonExistentId = UUID.randomUUID();

        // then
        mockMvc.perform(get("/api/v1/clients/{clientId}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CLIENT_NOT_FOUND"));
    }

    // PUT /clients/{clientId}
    @Test
    void updateClient_shouldReturn200AndUpdatedClient_whenValidRequest() throws Exception {
        // given
        createClient_shouldReturn201AndClient_whenValidRequest();
        String requestBody = objectMapper.writeValueAsString(updateRequest);

        // then
        mockMvc.perform(put("/api/v1/clients/{clientId}", createdClientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Петр"))
                .andExpect(jsonPath("$.lastName").value("Иванов"))
                .andExpect(jsonPath("$.middleName").value("Александрович"));
    }

    @Test
    void updateClient_shouldReturn404_whenClientDoesNotExist() throws Exception {
        // given
        UUID nonExistentId = UUID.randomUUID();
        String requestBody = objectMapper.writeValueAsString(updateRequest);

        // then
        mockMvc.perform(put("/api/v1/clients/{clientId}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CLIENT_NOT_FOUND"));
    }

    @Test
    void updateClient_shouldReturn400_whenFirstNameIsInvalid() throws Exception {
        // given
        createClient_shouldReturn201AndClient_whenValidRequest();
        updateRequest.setFirstName("");
        String requestBody = objectMapper.writeValueAsString(updateRequest);

        // then
        mockMvc.perform(put("/api/v1/clients/{clientId}", createdClientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void updateClient_shouldReturn400_whenLastNameIsInvalid() throws Exception {
        // given
        createClient_shouldReturn201AndClient_whenValidRequest();
        updateRequest.setLastName("a".repeat(101));
        String requestBody = objectMapper.writeValueAsString(updateRequest);

        // then
        mockMvc.perform(put("/api/v1/clients/{clientId}", createdClientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    // DELETE /clients/{clientId}
    @Test
    void deleteClient_shouldReturn204_whenClientExists() throws Exception {
        // given
        createClient_shouldReturn201AndClient_whenValidRequest();

        // when
        mockMvc.perform(delete("/api/v1/clients/{clientId}", createdClientId))
                .andExpect(status().isNoContent());

        // then
        mockMvc.perform(get("/api/v1/clients/{clientId}", createdClientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELETED"));
    }

    @Test
    void deleteClient_shouldReturn404_whenClientDoesNotExist() throws Exception {
        // given
        UUID nonExistentId = UUID.randomUUID();

        // then
        mockMvc.perform(delete("/api/v1/clients/{clientId}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CLIENT_NOT_FOUND"));
    }

    // GET /clients
    @Test
    void searchClients_shouldReturnPageOfClients() throws Exception {
        // given
        createClient_shouldReturn201AndClient_whenValidRequest();

        // then
        mockMvc.perform(get("/api/v1/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageable.pageNumber").value(0));
    }

    @Test
    void searchClients_shouldReturnPageWithCustomPageAndSize() throws Exception {
        // given
        createClient_shouldReturn201AndClient_whenValidRequest();

        // then
        mockMvc.perform(get("/api/v1/clients?page=1&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageable.pageNumber").value(1))
                .andExpect(jsonPath("$.pageable.pageSize").value(5));
    }

    @Test
    void searchClients_shouldFilterByLastName() throws Exception {
        // given
        createClient_shouldReturn201AndClient_whenValidRequest();

        // then
        mockMvc.perform(get("/api/v1/clients?lastName=Петров"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].lastName").value("Петров"));
    }

    @Test
    void searchClients_shouldFilterByMdmId() throws Exception {
        // given
        createClient_shouldReturn201AndClient_whenValidRequest();

        // then
        mockMvc.perform(get("/api/v1/clients?mdmId=1234567890"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].mdmId").value(1234567890L));
    }

    @Test
    void searchClients_shouldFilterByLastNameAndMdmId() throws Exception {
        // given
        createClient_shouldReturn201AndClient_whenValidRequest();

        // then
        mockMvc.perform(get("/api/v1/clients?lastName=Петров&mdmId=1234567890"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].lastName").value("Петров"))
                .andExpect(jsonPath("$.content[0].mdmId").value(1234567890L));
    }

    @Test
    void searchClients_shouldReturnEmptyPage_whenNoMatches() throws Exception {
        // given
        createClient_shouldReturn201AndClient_whenValidRequest();

        // then
        mockMvc.perform(get("/api/v1/clients?lastName=nonexistentlastname"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.pageable.totalElements").value(0));
    }

    @Test
    void searchClients_shouldUseDefaultPageAndSize_whenNotProvided() throws Exception {
        // given
        createClient_shouldReturn201AndClient_whenValidRequest();

        // then
        mockMvc.perform(get("/api/v1/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageable.pageNumber").value(DEFAULT_PAGE))
                .andExpect(jsonPath("$.pageable.pageSize").value(DEFAULT_SIZE));
    }

    // GET /clients/{clientId}/exists
    @Test
    void checkClientExists_shouldReturn200WithExistsTrue_whenClientExists() throws Exception {
        // given
        createClient_shouldReturn201AndClient_whenValidRequest();

        // then
        mockMvc.perform(get("/api/v1/clients/{clientId}/exists", createdClientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void checkClientExists_shouldReturn200WithExistsFalse_whenClientDoesNotExist() throws Exception {
        // given
        UUID nonExistentId = UUID.randomUUID();

        // then
        mockMvc.perform(get("/api/v1/clients/{clientId}/exists", nonExistentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));
    }

    // errors
    @Test
    void handleNoResourceFound_shouldReturn404WithErrorResponse_whenWrongUrl() throws Exception {
        // given
        String wrongUrl = "/api/v1/wrongpath";

        // then
        mockMvc.perform(get(wrongUrl))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
                .andExpect(jsonPath("$.errorDescription").value("Resource not found."))
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    @Test
    void handleMethodNotAllowed_shouldReturn405WithErrorResponse_whenWrongHttpMethod() throws Exception {
        // given
        createClient_shouldReturn201AndClient_whenValidRequest();

        // then
        mockMvc.perform(post("/api/v1/clients/{clientId}", createdClientId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.statusCode").value(405));
    }
}