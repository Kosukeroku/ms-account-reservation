package kosukeroku.ms_account_reservation.service;

import kosukeroku.ms_account_reservation.config.PaginationConstants;
import kosukeroku.ms_account_reservation.dto.*;
import kosukeroku.ms_account_reservation.exception.ClientAlreadyExistsException;
import kosukeroku.ms_account_reservation.exception.ClientNotFoundException;
import kosukeroku.ms_account_reservation.mapper.ClientMapper;
import kosukeroku.ms_account_reservation.model.Client;
import kosukeroku.ms_account_reservation.dto.ClientStatus;
import kosukeroku.ms_account_reservation.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;


    @Transactional
    public ClientResponse createClient(ClientCreateRequest request) {
        log.info("Creating client with mdmId: {}", request.getMdmId());

        if (clientRepository.existsByMdmId(request.getMdmId())) {
            throw new ClientAlreadyExistsException("Client with mdmId " + request.getMdmId() + " already exists");
        }

        Client client = clientMapper.toEntity(request);

        client.setStatus(ClientStatus.ACTIVE);
        client.setCreatedAt(LocalDateTime.now());
        client.setUpdatedAt(LocalDateTime.now());

        Client savedClient = clientRepository.save(client);
        log.info("Client created with id: {}", savedClient.getId());

        ClientResponse response = clientMapper.toResponse(savedClient);
        response.setCreatedAt(savedClient.getCreatedAt().atOffset(ZoneOffset.UTC));
        response.setUpdatedAt(savedClient.getUpdatedAt().atOffset(ZoneOffset.UTC));

        return response;
    }

    @Transactional(readOnly = true)
    public ClientDetailsResponse getClientById(UUID id) {
        log.info("Getting client by id: {}", id);
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException("Client not found with id: " + id));

        ClientDetailsResponse response = clientMapper.toDetailsResponse(client);
        response.setCreatedAt(client.getCreatedAt().atOffset(ZoneOffset.UTC));
        response.setUpdatedAt(client.getUpdatedAt().atOffset(ZoneOffset.UTC));

        log.info("Client found: {}", client.getId());
        return response;
    }

    @Transactional
    public ClientResponse updateClient(UUID id, ClientUpdateRequest request) {
        log.info("Updating client with id: {}", id);

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException("Client not found with id: " + id));

        clientMapper.updateEntity(request, client);
        client.setUpdatedAt(LocalDateTime.now());

        Client updatedClient = clientRepository.save(client);
        log.info("Client updated with id: {}", updatedClient.getId());

        ClientResponse response = clientMapper.toResponse(updatedClient);
        response.setCreatedAt(updatedClient.getCreatedAt().atOffset(ZoneOffset.UTC));
        response.setUpdatedAt(updatedClient.getUpdatedAt().atOffset(ZoneOffset.UTC));
        return response;
    }

    @Transactional
    public void deleteClient(UUID id) {
        log.info("Deleting client with id: {}", id);

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException("Client not found with id: " + id));

        client.setStatus(ClientStatus.DELETED);
        client.setUpdatedAt(LocalDateTime.now());
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
        int pageNum = page != null ? page : PaginationConstants.DEFAULT_PAGE;
        int pageSize = size != null ? size : PaginationConstants.DEFAULT_SIZE;

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
                    ClientResponse cr = clientMapper.toResponse(client);
                    cr.setCreatedAt(client.getCreatedAt().atOffset(ZoneOffset.UTC));
                    cr.setUpdatedAt(client.getUpdatedAt().atOffset(ZoneOffset.UTC));
                    return cr;
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
}