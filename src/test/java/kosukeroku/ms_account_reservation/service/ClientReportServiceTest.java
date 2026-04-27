package kosukeroku.ms_account_reservation.service;

import kosukeroku.ms_account_reservation.config.AsyncProperties;
import kosukeroku.ms_account_reservation.dto.ClientReportResponse;
import kosukeroku.ms_account_reservation.dto.ClientStatus;
import kosukeroku.ms_account_reservation.exception.ApiException;
import kosukeroku.ms_account_reservation.model.Client;
import kosukeroku.ms_account_reservation.model.enums.ErrorCode;
import kosukeroku.ms_account_reservation.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;


import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientReportServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ExchangeRateService exchangeRateService;

    @Mock
    private AsyncProperties asyncProperties;

    @Mock
    private ThreadPoolTaskExecutor executor;

    @InjectMocks
    private ClientReportService clientReportService;

    private UUID testId;
    private Client client;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        client = new Client();
        client.setId(testId);
        client.setFirstName("Иван");
        client.setLastName("Петров");
        client.setMiddleName("Сергеевич");
        client.setStatus(ClientStatus.ACTIVE);

        lenient().doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executor).execute(any(Runnable.class));
    }

    @Test
    void getClientReport_shouldReturnFullReport_whenAllDataAvailable() {
        // given
        when(clientRepository.findById(testId)).thenReturn(Optional.of(client));
        when(exchangeRateService.getExchangeRate("USD", "RUB")).thenReturn(new BigDecimal("75.0"));
        when(exchangeRateService.getExchangeRate("EUR", "RUB")).thenReturn(new BigDecimal("85.0"));

        // when
        ClientReportResponse result = clientReportService.getClientReport(testId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getClientId()).isEqualTo(testId);
        assertThat(result.getFirstName()).isEqualTo("Иван");
        assertThat(result.getLastName()).isEqualTo("Петров");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getExchangeRates())
                .containsEntry("USD/RUB", new BigDecimal("75.0"))
                .containsEntry("EUR/RUB", new BigDecimal("85.0"));
    }

    @Test
    void getClientReport_shouldReturnPartialReport_whenUsdRateFails() {
        // given
        when(clientRepository.findById(testId)).thenReturn(Optional.of(client));
        when(exchangeRateService.getExchangeRate("USD", "RUB"))
                .thenThrow(new RuntimeException("API error"));
        when(exchangeRateService.getExchangeRate("EUR", "RUB")).thenReturn(new BigDecimal("85.0"));

        // when
        ClientReportResponse result = clientReportService.getClientReport(testId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getExchangeRates())
                .containsEntry("EUR/RUB", new BigDecimal("85.0"))
                .doesNotContainKey("USD/RUB");
        assertThat(result.getExchangeRates().size()).isEqualTo(1);
    }

    @Test
    void getClientReport_shouldReturnPartialReport_whenEurRateFails() {
        // given
        when(clientRepository.findById(testId)).thenReturn(Optional.of(client));
        when(exchangeRateService.getExchangeRate("USD", "RUB")).thenReturn(new BigDecimal("75.0"));
        when(exchangeRateService.getExchangeRate("EUR", "RUB"))
                .thenThrow(new RuntimeException("API error"));

        // when
        ClientReportResponse result = clientReportService.getClientReport(testId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getExchangeRates())
                .containsEntry("USD/RUB", new BigDecimal("75.0"))
                .doesNotContainKey("EUR/RUB");
        assertThat(result.getExchangeRates().size()).isEqualTo(1);
    }

    @Test
    void getClientReport_shouldReturnEmptyRates_whenAllRatesFail() {
        // given
        when(clientRepository.findById(testId)).thenReturn(Optional.of(client));
        when(exchangeRateService.getExchangeRate("USD", "RUB"))
                .thenThrow(new RuntimeException("API error"));
        when(exchangeRateService.getExchangeRate("EUR", "RUB"))
                .thenThrow(new RuntimeException("API error"));

        // when
        ClientReportResponse result = clientReportService.getClientReport(testId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getExchangeRates()).isEmpty();
    }

    @Test
    void getClientReport_shouldThrowClientNotFound_evenIfOtherTasksRun_whenClientDoesNotExist() {
        // given
        when(clientRepository.findById(testId)).thenReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> clientReportService.getClientReport(testId))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CLIENT_NOT_FOUND);

        verify(clientRepository, times(1)).findById(testId);
    }

    @Test
    void getClientReport_shouldCallAllServicesExactlyOnce() {
        // given
        when(clientRepository.findById(testId)).thenReturn(Optional.of(client));
        when(exchangeRateService.getExchangeRate("USD", "RUB")).thenReturn(new BigDecimal("75.0"));
        when(exchangeRateService.getExchangeRate("EUR", "RUB")).thenReturn(new BigDecimal("85.0"));

        // when
        clientReportService.getClientReport(testId);

        // then
        verify(clientRepository, times(1)).findById(testId);
        verify(exchangeRateService, times(1)).getExchangeRate("USD", "RUB");
        verify(exchangeRateService, times(1)).getExchangeRate("EUR", "RUB");
    }

}