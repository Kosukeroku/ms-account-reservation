package kosukeroku.ms_account_reservation.service;

import kosukeroku.ms_account_reservation.dto.*;
import kosukeroku.ms_account_reservation.exception.ApiException;
import kosukeroku.ms_account_reservation.mapper.ClientMapper;
import kosukeroku.ms_account_reservation.model.Account;
import kosukeroku.ms_account_reservation.model.AccountStatus;
import kosukeroku.ms_account_reservation.model.Client;
import kosukeroku.ms_account_reservation.model.enums.AccountStatusName;
import kosukeroku.ms_account_reservation.model.enums.ErrorCode;
import kosukeroku.ms_account_reservation.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientMapper clientMapper;

    @InjectMocks
    private ClientService clientService;

    private UUID testId;
    private ClientCreateRequest createRequest;
    private ClientUpdateRequest updateRequest;
    private Client client;
    private ClientResponse clientResponse;
    private ClientDetailsResponse detailsResponse;
    private ClientSearchResponse searchResponse;
    private List<Account> accounts;

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();

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

        AccountStatus newStatus = new AccountStatus(AccountStatusName.NEW);
        newStatus.setId(1);
        AccountStatus createdStatus = new AccountStatus(AccountStatusName.CREATED);
        createdStatus.setId(2);

        Account account1 = new Account();
        account1.setId(UUID.randomUUID());
        account1.setStatus(newStatus);
        account1.setCurrencyCode("USD");
        account1.setBalance(new BigDecimal("1000.00"));
        account1.setAccountNumber("ACC001");

        Account account2 = new Account();
        account2.setId(UUID.randomUUID());
        account2.setStatus(createdStatus);
        account2.setCurrencyCode("EUR");
        account2.setBalance(new BigDecimal("5000.00"));
        account2.setAccountNumber("ACC002");

        accounts = List.of(account1, account2);

        client = new Client();
        client.setId(testId);
        client.setMdmId(1234567890L);
        client.setFirstName("Иван");
        client.setLastName("Петров");
        client.setMiddleName("Сергеевич");
        client.setCitizenship("РФ");
        client.setClientType("INDIVIDUAL");
        client.setDocumentNumber("123456");
        client.setDocumentSeries("1234");
        client.setDocumentType("PASSPORT");
        client.setStatus(ClientStatus.ACTIVE);
        client.setCreatedAt(LocalDateTime.now());
        client.setUpdatedAt(LocalDateTime.now());
        client.setAccounts(accounts);

        clientResponse = new ClientResponse();
        clientResponse.setId(testId);
        clientResponse.setMdmId(1234567890L);
        clientResponse.setFirstName("Иван");
        clientResponse.setLastName("Петров");
        clientResponse.setMiddleName("Сергеевич");
        clientResponse.setStatus(ClientStatus.ACTIVE);

        detailsResponse = new ClientDetailsResponse();
        detailsResponse.setId(testId);
        detailsResponse.setMdmId(1234567890L);
        detailsResponse.setFirstName("Иван");
        detailsResponse.setLastName("Петров");
        detailsResponse.setMiddleName("Сергеевич");
        detailsResponse.setStatus(ClientStatus.ACTIVE);

        searchResponse = new ClientSearchResponse();
        searchResponse.setId(testId);
        searchResponse.setMdmId(1234567890L);
        searchResponse.setFirstName("Иван");
        searchResponse.setLastName("Петров");
        searchResponse.setMiddleName("Сергеевич");
        searchResponse.setStatus(ClientStatus.ACTIVE);
        searchResponse.setActiveAccountsCount(accounts.size());
    }

    // create client
    @Test
    void createClient_shouldReturnClientResponse_whenAllFieldsAreValid() {
        // given
        when(clientRepository.existsByMdmId(1234567890L)).thenReturn(false);
        when(clientMapper.toEntity(createRequest)).thenReturn(client);
        when(clientRepository.save(client)).thenReturn(client);
        when(clientMapper.toResponse(client)).thenReturn(clientResponse);

        // when
        ClientResponse result = clientService.createClient(createRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testId);
        assertThat(result.getMdmId()).isEqualTo(1234567890L);
        assertThat(result.getFirstName()).isEqualTo("Иван");
        assertThat(result.getLastName()).isEqualTo("Петров");
        assertThat(result.getMiddleName()).isEqualTo("Сергеевич");
        assertThat(result.getStatus()).isEqualTo(ClientStatus.ACTIVE);

        verify(clientRepository).existsByMdmId(1234567890L);
        verify(clientMapper).toEntity(createRequest);
        verify(clientRepository).save(client);
        verify(clientMapper).toResponse(client);
    }

    @Test
    void createClient_shouldThrowApiException_whenMdmIdAlreadyExists() {
        // given
        when(clientRepository.existsByMdmId(1234567890L)).thenReturn(true);

        // then
        assertThatThrownBy(() -> clientService.createClient(createRequest))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getErrorCode()).isEqualTo(ErrorCode.CLIENT_ALREADY_EXISTS);
                    assertThat(apiEx.getMessage()).contains("Client with mdmId 1234567890 already exists");
                });

        verify(clientRepository).existsByMdmId(1234567890L);
        verify(clientMapper, never()).toEntity(any());
        verify(clientRepository, never()).save(any());
    }

    // get client by id
    @Test
    void getClientById_shouldReturnClientDetailsResponse_whenClientExists() {
        // given
        when(clientRepository.findById(testId)).thenReturn(Optional.of(client));

        AccountResponse accountResponse1 = new AccountResponse();
        accountResponse1.setCurrencyCode("USD");
        accountResponse1.setBalance(new BigDecimal("1000.00"));

        AccountResponse accountResponse2 = new AccountResponse();
        accountResponse2.setCurrencyCode("EUR");
        accountResponse2.setBalance(new BigDecimal("5000.00"));

        detailsResponse.setAccounts(List.of(accountResponse1, accountResponse2));
        when(clientMapper.toDetailsResponse(client)).thenReturn(detailsResponse);

        // when
        ClientDetailsResponse result = clientService.getClientById(testId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testId);
        assertThat(result.getMdmId()).isEqualTo(1234567890L);
        assertThat(result.getFirstName()).isEqualTo("Иван");
        assertThat(result.getLastName()).isEqualTo("Петров");

        assertThat(result.getAccounts()).isNotNull();
        assertThat(result.getAccounts()).hasSize(2);
        assertThat(result.getAccounts().get(0).getCurrencyCode()).isEqualTo("USD");

        verify(clientRepository).findById(testId);
        verify(clientMapper).toDetailsResponse(client);
    }

    @Test
    void getClientById_shouldThrowApiException_whenClientDoesNotExist() {
        // given
        when(clientRepository.findById(testId)).thenReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> clientService.getClientById(testId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getErrorCode()).isEqualTo(ErrorCode.CLIENT_NOT_FOUND);
                    assertThat(apiEx.getMessage()).contains("Client not found with id: " + testId);
                });

        verify(clientRepository).findById(testId);
        verify(clientMapper, never()).toDetailsResponse(any());
    }

    // search clients
    @Test
    void searchClients_shouldReturnPageResponse_whenNoFilters() {
        // given
        Page<Client> clientPage = new PageImpl<>(List.of(client), PageRequest.of(0, 20), 1);
        when(clientRepository.findAll(any(Pageable.class))).thenReturn(clientPage);
        when(clientMapper.toSearchResponse(client)).thenReturn(searchResponse);

        // when
        ClientPageResponse result = clientService.searchClients(null, null, null, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getPageable().getPageNumber()).isEqualTo(DEFAULT_PAGE);
        assertThat(result.getPageable().getPageSize()).isEqualTo(DEFAULT_SIZE);

        verify(clientRepository).findAll(any(Pageable.class));
        verify(clientMapper).toSearchResponse(client);
    }

    @Test
    void searchClients_shouldReturnPageResponse_whenFilterByLastName() {
        // given
        Page<Client> clientPage = new PageImpl<>(List.of(client), PageRequest.of(0, 20), 1);
        when(clientRepository.findByLastNameContainingIgnoreCase(eq("Петров"), any(Pageable.class)))
                .thenReturn(clientPage);
        when(clientMapper.toSearchResponse(client)).thenReturn(searchResponse);

        // when
        ClientPageResponse result = clientService.searchClients("Петров", null, null, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(clientRepository).findByLastNameContainingIgnoreCase(eq("Петров"), any(Pageable.class));
        verify(clientMapper).toSearchResponse(client);
    }

    @Test
    void searchClients_shouldReturnPageResponse_whenFilterByMdmId() {
        // given
        Page<Client> clientPage = new PageImpl<>(List.of(client), PageRequest.of(0, 20), 1);
        when(clientRepository.findByMdmId(eq(1234567890L), any(Pageable.class)))
                .thenReturn(clientPage);
        when(clientMapper.toSearchResponse(client)).thenReturn(searchResponse);

        // when
        ClientPageResponse result = clientService.searchClients(null, 1234567890L, null, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(clientRepository).findByMdmId(eq(1234567890L), any(Pageable.class));
        verify(clientMapper).toSearchResponse(client);
    }

    @Test
    void searchClients_shouldReturnPageResponse_whenFilterByLastNameAndMdmId() {
        // given
        Page<Client> clientPage = new PageImpl<>(List.of(client), PageRequest.of(0, 20), 1);
        when(clientRepository.findByLastNameContainingIgnoreCaseAndMdmId(eq("Петров"), eq(1234567890L), any(Pageable.class)))
                .thenReturn(clientPage);
        when(clientMapper.toSearchResponse(client)).thenReturn(searchResponse);

        // when
        ClientPageResponse result = clientService.searchClients("Петров", 1234567890L, null, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(clientRepository).findByLastNameContainingIgnoreCaseAndMdmId(eq("Петров"), eq(1234567890L), any(Pageable.class));
        verify(clientMapper).toSearchResponse(client);
    }

    @Test
    void searchClients_shouldUseCustomPageAndSize() {
        // given
        Page<Client> clientPage = new PageImpl<>(List.of(client), PageRequest.of(2, 10), 1);
        when(clientRepository.findAll(PageRequest.of(2, 10))).thenReturn(clientPage);
        when(clientMapper.toSearchResponse(client)).thenReturn(searchResponse);

        // when
        ClientPageResponse result = clientService.searchClients(null, null, 2, 10);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPageable().getPageNumber()).isEqualTo(2);
        assertThat(result.getPageable().getPageSize()).isEqualTo(10);

        verify(clientRepository).findAll(PageRequest.of(2, 10));
        verify(clientMapper).toSearchResponse(client);
    }

    @Test
    void searchClients_shouldReturnEmptyPage_whenNoClientsFound() {
        // given
        Page<Client> emptyPage = Page.empty();
        when(clientRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        // when
        ClientPageResponse result = clientService.searchClients(null, null, null, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getPageable().getTotalElements()).isZero();
    }

    @Test
    void searchClients_shouldReturnActiveAccountsCount() {
        // given
        Page<Client> clientPage = new PageImpl<>(List.of(client), PageRequest.of(0, 20), 1);
        when(clientRepository.findByLastNameContainingIgnoreCase(eq("Петров"), any(Pageable.class)))
                .thenReturn(clientPage);
        when(clientMapper.toSearchResponse(client)).thenReturn(searchResponse);

        // when
        ClientPageResponse result = clientService.searchClients("Петров", null, null, null);

        // then
        assertThat(result).isNotNull();
        ClientSearchResponse response = (ClientSearchResponse) result.getContent().get(0);
        assertThat(response.getActiveAccountsCount()).isEqualTo(2);
    }

    @Test
    void searchClients_shouldReturnZeroActiveAccountsCount_whenClientHasNoAccounts() {
        // given
        Client clientWithoutAccounts = new Client();
        clientWithoutAccounts.setId(testId);
        clientWithoutAccounts.setFirstName("Иван");
        clientWithoutAccounts.setLastName("Петров");
        clientWithoutAccounts.setAccounts(new ArrayList<>());

        ClientSearchResponse emptySearchResponse = new ClientSearchResponse();
        emptySearchResponse.setId(testId);
        emptySearchResponse.setActiveAccountsCount(0);

        Page<Client> clientPage = new PageImpl<>(List.of(clientWithoutAccounts), PageRequest.of(0, 20), 1);
        when(clientRepository.findByLastNameContainingIgnoreCase(eq("Петров"), any(Pageable.class)))
                .thenReturn(clientPage);
        when(clientMapper.toSearchResponse(clientWithoutAccounts)).thenReturn(emptySearchResponse);

        // when
        ClientPageResponse result = clientService.searchClients("Петров", null, null, null);

        // then
        assertThat(result).isNotNull();
        ClientSearchResponse response = (ClientSearchResponse) result.getContent().get(0);
        assertThat(response.getActiveAccountsCount()).isZero();
    }

    // update client
    @Test
    void updateClient_shouldReturnUpdatedClientResponse_whenClientExistsAndOnlyThreeFieldsUpdated() {
        // given
        Client updatedClient = new Client();
        updatedClient.setId(testId);
        updatedClient.setMdmId(1234567890L);
        updatedClient.setFirstName("Петр");
        updatedClient.setLastName("Иванов");
        updatedClient.setMiddleName("Александрович");
        updatedClient.setStatus(ClientStatus.ACTIVE);
        updatedClient.setCreatedAt(LocalDateTime.now());
        updatedClient.setUpdatedAt(LocalDateTime.now());

        ClientResponse updatedResponse = new ClientResponse();
        updatedResponse.setId(testId);
        updatedResponse.setMdmId(1234567890L);
        updatedResponse.setFirstName("Петр");
        updatedResponse.setLastName("Иванов");
        updatedResponse.setMiddleName("Александрович");
        updatedResponse.setStatus(ClientStatus.ACTIVE);

        when(clientRepository.findById(testId)).thenReturn(Optional.of(client));
        when(clientRepository.save(client)).thenReturn(updatedClient);
        when(clientMapper.toResponse(updatedClient)).thenReturn(updatedResponse);

        // when
        ClientResponse result = clientService.updateClient(testId, updateRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("Петр");
        assertThat(result.getLastName()).isEqualTo("Иванов");
        assertThat(result.getMiddleName()).isEqualTo("Александрович");

        verify(clientMapper).updateEntity(updateRequest, client);
        verify(clientRepository).save(client);
    }

    @Test
    void updateClient_shouldThrowApiException_whenClientDoesNotExist() {
        // given
        when(clientRepository.findById(testId)).thenReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> clientService.updateClient(testId, updateRequest))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getErrorCode()).isEqualTo(ErrorCode.CLIENT_NOT_FOUND);
                    assertThat(apiEx.getMessage()).contains("Client not found with id: " + testId);
                });

        verify(clientRepository).findById(testId);
        verify(clientMapper, never()).updateEntity(any(), any());
        verify(clientRepository, never()).save(any());
    }

    // soft delete client
    @Test
    void deleteClient_shouldSoftDeleteClient_whenClientExistsAndNoAccounts() {
        // given
        Client clientWithoutAccounts = new Client();
        clientWithoutAccounts.setId(testId);
        clientWithoutAccounts.setStatus(ClientStatus.ACTIVE);
        clientWithoutAccounts.setAccounts(new ArrayList<>());

        when(clientRepository.findById(testId)).thenReturn(Optional.of(clientWithoutAccounts));
        when(clientRepository.save(clientWithoutAccounts)).thenReturn(clientWithoutAccounts);

        // when
        clientService.deleteClient(testId);

        // then
        assertThat(clientWithoutAccounts.getStatus()).isEqualTo(ClientStatus.DELETED);
        verify(clientRepository).save(clientWithoutAccounts);
    }

    @Test
    void deleteClient_shouldThrowApiException_whenClientDoesNotExist() {
        // given
        when(clientRepository.findById(testId)).thenReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> clientService.deleteClient(testId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getErrorCode()).isEqualTo(ErrorCode.CLIENT_NOT_FOUND);
                    assertThat(apiEx.getMessage()).contains("Client not found with id: " + testId);
                });

        verify(clientRepository, never()).save(any());
    }

    // check if client exists
    @Test
    void checkClientExists_shouldReturnExistsTrue_whenClientExists() {
        // given
        when(clientRepository.existsById(testId)).thenReturn(true);
        when(clientRepository.findById(testId)).thenReturn(Optional.of(client));

        // when
        ClientExistsResponse result = clientService.checkClientExists(testId);

        // then
        assertThat(result.getExists()).isTrue();
        assertThat(result.getClientId()).isEqualTo(testId);
        assertThat(result.getStatus()).isEqualTo(ClientStatus.ACTIVE);

        verify(clientRepository).existsById(testId);
        verify(clientRepository).findById(testId);
    }

    @Test
    void checkClientExists_shouldReturnExistsFalse_whenClientDoesNotExist() {
        // given
        when(clientRepository.existsById(testId)).thenReturn(false);

        // when
        ClientExistsResponse result = clientService.checkClientExists(testId);

        // then
        assertThat(result.getExists()).isFalse();
        assertThat(result.getClientId()).isNull();
        assertThat(result.getStatus()).isNull();

        verify(clientRepository).existsById(testId);
        verify(clientRepository, never()).findById(any());
    }
}