package kosukeroku.ms_account_reservation.service;

import kosukeroku.ms_account_reservation.config.AsyncProperties;
import kosukeroku.ms_account_reservation.dto.ClientReportResponse;
import kosukeroku.ms_account_reservation.exception.ApiException;
import kosukeroku.ms_account_reservation.model.Client;
import kosukeroku.ms_account_reservation.model.enums.ErrorCode;
import kosukeroku.ms_account_reservation.repository.ClientRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ClientReportService {

    private final ClientRepository clientRepository;
    private final ExchangeRateService exchangeRateService;
    private final AsyncProperties asyncProperties;
    private final ThreadPoolTaskExecutor executor;

    public ClientReportService(
            ClientRepository clientRepository,
            ExchangeRateService exchangeRateService,
            AsyncProperties asyncProperties,
            @Qualifier("reportTaskExecutor") ThreadPoolTaskExecutor executor
    ) {
        this.clientRepository = clientRepository;
        this.exchangeRateService = exchangeRateService;
        this.asyncProperties = asyncProperties;
        this.executor = executor;
    }

    public ClientReportResponse getClientReport(UUID clientId) {
        CompletableFuture<Client> clientFuture = fetchClientAsync(clientId);
        CompletableFuture<BigDecimal> usdRateFuture = fetchRateAsync("USD", "RUB");
        CompletableFuture<BigDecimal> eurRateFuture = fetchRateAsync("EUR", "RUB");

        try {
            CompletableFuture.allOf(clientFuture, usdRateFuture, eurRateFuture).join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof ApiException) {
                throw (ApiException) e.getCause();
            }
            throw new ApiException(ErrorCode.GATEWAY_TIMEOUT, "Client service timeout or unavailable");
        }

        Client client = clientFuture.join();
        BigDecimal usdRate = usdRateFuture.getNow(null);
        BigDecimal eurRate = eurRateFuture.getNow(null);

        Map<String, BigDecimal> rates = new HashMap<>();
        if (usdRate != null) rates.put("USD/RUB", usdRate);
        if (eurRate != null) rates.put("EUR/RUB", eurRate);

        return ClientReportResponse.builder()
                .clientId(client.getId())
                .firstName(client.getFirstName())
                .lastName(client.getLastName())
                .status(client.getStatus().name())
                .exchangeRates(rates)
                .build();
    }

    private CompletableFuture<Client> fetchClientAsync(UUID clientId) {
        return CompletableFuture
                .supplyAsync(() -> clientRepository.findById(clientId)
                        .orElseThrow(() -> new ApiException(ErrorCode.CLIENT_NOT_FOUND,
                                "Client not found with id: " + clientId)), executor)
                .orTimeout(asyncProperties.getTimeoutSeconds(), TimeUnit.SECONDS)
                .thenApply(client -> {
                    if (client == null) {
                        throw new ApiException(ErrorCode.GATEWAY_TIMEOUT,
                                "Client service timeout or unavailable");
                    }
                    return client;
                });
    }

    private CompletableFuture<BigDecimal> fetchRateAsync(String from, String to) {
        return CompletableFuture
                .supplyAsync(() -> exchangeRateService.getExchangeRate(from, to), executor)
                .exceptionally(ex -> {
                    log.warn("Failed to get {}/{} rate: {}", from, to, ex.getMessage());
                    return null;
                });
    }
}