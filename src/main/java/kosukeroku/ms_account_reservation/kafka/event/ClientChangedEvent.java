package kosukeroku.ms_account_reservation.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientChangedEvent {
    private UUID clientId;
    private EventType eventType;
    private Instant timestamp;
    private String eventId;
}