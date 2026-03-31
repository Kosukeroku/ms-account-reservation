package kosukeroku.ms_account_reservation.controller;

import kosukeroku.ms_account_reservation.api.ClientsApi;
import kosukeroku.ms_account_reservation.dto.*;
import kosukeroku.ms_account_reservation.service.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class ClientController implements ClientsApi {

    private final ClientService clientService;

    @Override
    public ResponseEntity<ClientResponse> createClient(ClientCreateRequest request) {
        return new ResponseEntity<>(clientService.createClient(request), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<ClientDetailsResponse> getClientById(UUID clientId) {
        return ResponseEntity.ok(clientService.getClientById(clientId));
    }

    @Override
    public ResponseEntity<ClientResponse> updateClient(UUID clientId, ClientUpdateRequest request) {
        return ResponseEntity.ok(clientService.updateClient(clientId, request));
    }

    @Override
    public ResponseEntity<Void> deleteClient(UUID clientId) {
        clientService.deleteClient(clientId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ClientPageResponse> searchClients(Integer page, Integer size, String lastName, Long mdmId) {
        return ResponseEntity.ok(clientService.searchClients(lastName, mdmId, page, size));
    }

    @Override
    public ResponseEntity<ClientExistsResponse> checkClientExists(UUID clientId) {
        return ResponseEntity.ok(clientService.checkClientExists(clientId));
    }
}