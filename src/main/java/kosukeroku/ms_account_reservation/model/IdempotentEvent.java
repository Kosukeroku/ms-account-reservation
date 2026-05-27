package kosukeroku.ms_account_reservation.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "idempotent_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdempotentEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", unique = true, nullable = false)
    private String eventId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}