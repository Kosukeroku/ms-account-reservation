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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;

    @Transactional
    public ClientResponse createClient(ClientCreateRequest request) {
        log.info("Creating client with mdmId: {}", request.getMdmId());

        if (clientRepository.existsByMdmId(request.getMdmId())) {
            throw new ApiException(ErrorCode.CLIENT_ALREADY_EXISTS,
                    "Client with mdmId " + request.getMdmId() + " already exists");
        }
        Client client = clientMapper.toEntity(request);
        Client savedClient = clientRepository.save(client);
        log.info("Client created with id: {}", savedClient.getId());

        return clientMapper.toResponse(savedClient);
    }

    @Transactional(readOnly = true)
    public ClientDetailsResponse getClientById(UUID id) {
        log.info("Getting client by id: {}", id);

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.CLIENT_NOT_FOUND,
                        "Client not found with id: " + id));

        log.info("Client found: {}, accounts count: {}",
                client.getId(),
                client.getAccounts() != null ? client.getAccounts().size() : 0);

        return clientMapper.toDetailsResponse(client);
    }

    @Transactional
    public ClientResponse updateClient(UUID id, ClientUpdateRequest request) {
        log.info("Updating client with id: {}", id);

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.CLIENT_NOT_FOUND,
                        "Client not found with id: " + id));

        clientMapper.updateEntity(request, client);
        Client updatedClient = clientRepository.save(client);
        log.info("Client updated with id: {}", updatedClient.getId());

        return clientMapper.toResponse(updatedClient);
    }

    @Transactional
    public void deleteClient(UUID id) {
        log.info("Deleting client with id: {}", id);

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.CLIENT_NOT_FOUND,
                        "Client not found with id: " + id));

        boolean hasActiveAccounts = hasActiveAccounts(client);

        if (hasActiveAccounts) {
            throw new ApiException(ErrorCode.CLIENT_HAS_ACCOUNTS,
                    "Client has active accounts and cannot be deleted");
        }

        client.setStatus(ClientStatus.DELETED);
        clientRepository.save(client);
        log.info("Client deleted with id: {}", id);
    }

    @Transactional(readOnly = true)
    public ClientExistsResponse checkClientExists(UUID id) {
        log.info("Checking if client exists with id: {}", id);

        ClientExistsResponse response = new ClientExistsResponse();
        response.setExists(clientRepository.existsById(id));

        if (response.getExists()) {
            Client client = clientRepository.findById(id).get();
            response.setClientId(id);
            response.setStatus(client.getStatus());
            log.info("Client exists with id: {}", id);
        } else {
            log.info("Client does not exist with id: {}", id);
        }
        return response;
    }

    @Transactional(readOnly = true)
    public ClientPageResponse searchClients(String lastName, Long mdmId, Integer page, Integer size) {
        int pageNum = page != null ? page : DEFAULT_PAGE;
        int pageSize = size != null ? size : DEFAULT_SIZE;

        log.info("Searching clients: lastName={}, mdmId={}, page={}, size={}", lastName, mdmId, pageNum, pageSize);
        Pageable pageable = PageRequest.of(pageNum, pageSize);

        Page<Client> clientPage;

        if (lastName != null && mdmId != null) {
            clientPage = clientRepository.findByLastNameContainingIgnoreCaseAndMdmId(lastName, mdmId, pageable);
        } else if (lastName != null) {
            clientPage = clientRepository.findByLastNameContainingIgnoreCase(lastName, pageable);
        } else if (mdmId != null) {
            clientPage = clientRepository.findByMdmId(mdmId, pageable);
        } else {
            clientPage = clientRepository.findAll(pageable);
        }

        ClientPageResponse response = new ClientPageResponse();
        response.setContent(clientPage.getContent().stream()
                .map(client -> {
                    ClientSearchResponse searchResponse = clientMapper.toSearchResponse(client);
                    searchResponse.setActiveAccountsCount(countActiveAccounts(client));
                    return searchResponse;
                })
                .toList());

        ClientPageResponsePageable pageableResponse = new ClientPageResponsePageable();
        pageableResponse.setPageNumber(clientPage.getNumber());
        pageableResponse.setPageSize(clientPage.getSize());
        pageableResponse.setTotalPages(clientPage.getTotalPages());
        pageableResponse.setTotalElements((int) clientPage.getTotalElements());
        response.setPageable(pageableResponse);

        return response;
    }

    private int countActiveAccounts(Client client) {
        if (client.getAccounts() == null) {
            return 0;
        }
        return (int) client.getAccounts().stream()
                .map(Account::getStatus)
                .map(AccountStatus::getName)
                .filter(status -> status == AccountStatusName.NEW
                        || status == AccountStatusName.IN_CREATION
                        || status == AccountStatusName.CREATED)
                .count();
    }

    private boolean hasActiveAccounts(Client client) {
        return countActiveAccounts(client) > 0;
    }
}