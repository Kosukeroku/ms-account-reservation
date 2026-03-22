package kosukeroku.ms_account_reservation.service;

import kosukeroku.ms_account_reservation.config.PaginationConstants;
import kosukeroku.ms_account_reservation.dto.*;
import kosukeroku.ms_account_reservation.exception.ClientAlreadyExistsException;
import kosukeroku.ms_account_reservation.exception.ClientNotFoundException;
import kosukeroku.ms_account_reservation.mapper.ClientMapper;
import kosukeroku.ms_account_reservation.model.Client;
import kosukeroku.ms_account_reservation.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
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
        client.setCreatedAt(LocalDateTime.now());
        client.setUpdatedAt(LocalDateTime.now());

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
        detailsResponse.setHasAccounts(false);
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
    void createClient_shouldThrowClientAlreadyExistsException_whenMdmIdAlreadyExists() {
        // given
        when(clientRepository.existsByMdmId(1234567890L)).thenReturn(true);

        // then
        assertThatThrownBy(() -> clientService.createClient(createRequest))
                .isInstanceOf(ClientAlreadyExistsException.class)
                .hasMessageContaining("Client with mdmId 1234567890 already exists");

        verify(clientRepository).existsByMdmId(1234567890L);
        verify(clientMapper, never()).toEntity(any());
        verify(clientRepository, never()).save(any());
    }


    // get client by id
    @Test
    void getClientById_shouldReturnClientDetailsResponse_whenClientExists() {
        // given
        when(clientRepository.findById(testId)).thenReturn(Optional.of(client));
        when(clientMapper.toDetailsResponse(client)).thenReturn(detailsResponse);

        // when
        ClientDetailsResponse result = clientService.getClientById(testId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testId);
        assertThat(result.getMdmId()).isEqualTo(1234567890L);
        assertThat(result.getFirstName()).isEqualTo("Иван");
        assertThat(result.getLastName()).isEqualTo("Петров");
        assertThat(result.getHasAccounts()).isFalse();

        verify(clientRepository).findById(testId);
        verify(clientMapper).toDetailsResponse(client);
    }

    @Test
    void getClientById_shouldThrowClientNotFoundException_whenClientDoesNotExist() {
        // given
        when(clientRepository.findById(testId)).thenReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> clientService.getClientById(testId))
                .isInstanceOf(ClientNotFoundException.class)
                .hasMessageContaining("Client not found with id: " + testId);

        verify(clientRepository).findById(testId);
        verify(clientMapper, never()).toDetailsResponse(any());
    }

    // search clients
    @Test
    void searchClients_shouldReturnPageResponse_whenNoFilters() {
        // given
        Page<Client> clientPage = mock(Page.class);
        when(clientRepository.findAll(any(Pageable.class))).thenReturn(clientPage);
        when(clientPage.getContent()).thenReturn(List.of(client));
        when(clientPage.getNumber()).thenReturn(PaginationConstants.DEFAULT_PAGE);
        when(clientPage.getSize()).thenReturn(PaginationConstants.DEFAULT_SIZE);
        when(clientPage.getTotalPages()).thenReturn(1);
        when(clientPage.getTotalElements()).thenReturn(1L);
        when(clientMapper.toResponse(any())).thenReturn(clientResponse);

        // when
        ClientPageResponse result = clientService.searchClients(null, null, null, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        ClientResponse firstClient = result.getContent().getFirst();
        assertThat(firstClient.getId()).isEqualTo(testId);
        assertThat(firstClient.getMdmId()).isEqualTo(1234567890L);
        assertThat(firstClient.getFirstName()).isEqualTo("Иван");
        assertThat(firstClient.getLastName()).isEqualTo("Петров");

        assertThat(result.getPageable().getPageNumber()).isEqualTo(PaginationConstants.DEFAULT_PAGE);
        assertThat(result.getPageable().getPageSize()).isEqualTo(PaginationConstants.DEFAULT_SIZE);
        verify(clientRepository).findAll(any(Pageable.class));
    }

    @Test
    void searchClients_shouldReturnPageResponse_whenFilterByLastName() {
        // given
        Page<Client> clientPage = mock(Page.class);
        when(clientRepository.findByLastNameContainingIgnoreCase(eq("Петров"), any(Pageable.class)))
                .thenReturn(clientPage);
        when(clientPage.getContent()).thenReturn(List.of(client));
        when(clientPage.getNumber()).thenReturn(PaginationConstants.DEFAULT_PAGE);
        when(clientPage.getSize()).thenReturn(PaginationConstants.DEFAULT_SIZE);
        when(clientPage.getTotalPages()).thenReturn(1);
        when(clientPage.getTotalElements()).thenReturn(1L);
        when(clientMapper.toResponse(any())).thenReturn(clientResponse);

        // when
        ClientPageResponse result = clientService.searchClients("Петров", null, null, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        ClientResponse firstClient = result.getContent().getFirst();
        assertThat(firstClient.getId()).isEqualTo(testId);
        assertThat(firstClient.getMdmId()).isEqualTo(1234567890L);
        assertThat(firstClient.getFirstName()).isEqualTo("Иван");
        assertThat(firstClient.getLastName()).isEqualTo("Петров");

        verify(clientRepository).findByLastNameContainingIgnoreCase(eq("Петров"), any(Pageable.class));
    }

    @Test
    void searchClients_shouldReturnPageResponse_whenFilterByMdmId() {
        // given
        Page<Client> clientPage = mock(Page.class);
        when(clientRepository.findByMdmId(eq(1234567890L), any(Pageable.class)))
                .thenReturn(clientPage);
        when(clientPage.getContent()).thenReturn(List.of(client));
        when(clientPage.getNumber()).thenReturn(PaginationConstants.DEFAULT_PAGE);
        when(clientPage.getSize()).thenReturn(PaginationConstants.DEFAULT_SIZE);
        when(clientPage.getTotalPages()).thenReturn(1);
        when(clientPage.getTotalElements()).thenReturn(1L);
        when(clientMapper.toResponse(any())).thenReturn(clientResponse);

        // when
        ClientPageResponse result = clientService.searchClients(null, 1234567890L, null, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        ClientResponse firstClient = result.getContent().getFirst();
        assertThat(firstClient.getId()).isEqualTo(testId);
        assertThat(firstClient.getMdmId()).isEqualTo(1234567890L);
        assertThat(firstClient.getFirstName()).isEqualTo("Иван");
        assertThat(firstClient.getLastName()).isEqualTo("Петров");

        verify(clientRepository).findByMdmId(eq(1234567890L), any(Pageable.class));
    }

    @Test
    void searchClients_shouldReturnPageResponse_whenFilterByLastNameAndMdmId() {
        // given
        Page<Client> clientPage = mock(Page.class);
        when(clientRepository.findByLastNameContainingIgnoreCaseAndMdmId(eq("Петров"), eq(1234567890L), any(Pageable.class)))
                .thenReturn(clientPage);
        when(clientPage.getContent()).thenReturn(List.of(client));
        when(clientPage.getNumber()).thenReturn(PaginationConstants.DEFAULT_PAGE);
        when(clientPage.getSize()).thenReturn(PaginationConstants.DEFAULT_SIZE);
        when(clientPage.getTotalPages()).thenReturn(1);
        when(clientPage.getTotalElements()).thenReturn(1L);
        when(clientMapper.toResponse(any())).thenReturn(clientResponse);

        // when
        ClientPageResponse result = clientService.searchClients("Петров", 1234567890L, null, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        ClientResponse firstClient = result.getContent().getFirst();
        assertThat(firstClient.getId()).isEqualTo(testId);
        assertThat(firstClient.getMdmId()).isEqualTo(1234567890L);
        assertThat(firstClient.getFirstName()).isEqualTo("Иван");
        assertThat(firstClient.getLastName()).isEqualTo("Петров");

        verify(clientRepository).findByLastNameContainingIgnoreCaseAndMdmId(eq("Петров"), eq(1234567890L), any(Pageable.class));
    }

    @Test
    void searchClients_shouldUseCustomPageAndSize() {
        // given
        Page<Client> clientPage = mock(Page.class);
        when(clientRepository.findAll(any(Pageable.class))).thenReturn(clientPage);
        when(clientPage.getContent()).thenReturn(List.of(client));
        when(clientPage.getNumber()).thenReturn(2);
        when(clientPage.getSize()).thenReturn(10);
        when(clientPage.getTotalPages()).thenReturn(5);
        when(clientPage.getTotalElements()).thenReturn(50L);
        when(clientMapper.toResponse(any())).thenReturn(clientResponse);

        // when
        ClientPageResponse result = clientService.searchClients(null, null, 2, 10);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        ClientResponse firstClient = result.getContent().getFirst();
        assertThat(firstClient.getId()).isEqualTo(testId);
        assertThat(firstClient.getMdmId()).isEqualTo(1234567890L);
        assertThat(firstClient.getFirstName()).isEqualTo("Иван");
        assertThat(firstClient.getLastName()).isEqualTo("Петров");

        assertThat(result.getPageable().getPageNumber()).isEqualTo(2);
        assertThat(result.getPageable().getPageSize()).isEqualTo(10);
        assertThat(result.getPageable().getTotalPages()).isEqualTo(5);
        assertThat(result.getPageable().getTotalElements()).isEqualTo(50);

        verify(clientRepository).findAll(PageRequest.of(2, 10));
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
        updatedClient.setCitizenship("РФ");
        updatedClient.setClientType("INDIVIDUAL");
        updatedClient.setDocumentNumber("123456");
        updatedClient.setDocumentSeries("1234");
        updatedClient.setDocumentType("PASSPORT");
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
    void updateClient_shouldThrowClientNotFoundException_whenClientDoesNotExist() {
        // given
        when(clientRepository.findById(testId)).thenReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> clientService.updateClient(testId, updateRequest))
                .isInstanceOf(ClientNotFoundException.class)
                .hasMessageContaining("Client not found with id: " + testId);

        verify(clientRepository).findById(testId);
        verify(clientMapper, never()).updateEntity(any(), any());
        verify(clientRepository, never()).save(any());
    }

    // soft delete client
    @Test
    void deleteClient_shouldSoftDeleteClient_whenClientExists() {
        // given
        when(clientRepository.findById(testId)).thenReturn(Optional.of(client));
        when(clientRepository.save(client)).thenReturn(client);

        // when
        clientService.deleteClient(testId);

        // then
        assertThat(client.getStatus()).isEqualTo(ClientStatus.DELETED);
        verify(clientRepository).save(client);
    }

    @Test
    void deleteClient_shouldThrowClientNotFoundException_whenClientDoesNotExist() {
        // given
        when(clientRepository.findById(testId)).thenReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> clientService.deleteClient(testId))
                .isInstanceOf(ClientNotFoundException.class)
                .hasMessageContaining("Client not found with id: " + testId);

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