package kosukeroku.ms_account_reservation.kafka.service;

import kosukeroku.ms_account_reservation.model.IdempotentEvent;
import kosukeroku.ms_account_reservation.kafka.repository.IdempotentEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdempotentEventService {

    private final IdempotentEventRepository repository;

    @Transactional
    public boolean isProcessed(String eventId) {
        return repository.existsByEventId(eventId);
    }

    @Transactional
    public void saveProcessedEvent(String eventId, UUID clientId, String eventType) {
        IdempotentEvent record = new IdempotentEvent();
        record.setEventId(eventId);
        record.setClientId(clientId);
        record.setEventType(eventType);
        record.setProcessedAt(LocalDateTime.now());
        repository.save(record);
    }
}