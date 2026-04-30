package kosukeroku.ms_account_reservation.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class ClientReportResponse {
    private UUID clientId;
    private String firstName;
    private String lastName;
    private String status;
    private Map<String, BigDecimal> exchangeRates;
}