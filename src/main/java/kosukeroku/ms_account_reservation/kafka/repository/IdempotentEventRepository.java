package kosukeroku.ms_account_reservation.kafka.repository;

import kosukeroku.ms_account_reservation.model.IdempotentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface IdempotentEventRepository extends JpaRepository<IdempotentEvent, Long> {
    boolean existsByEventId(String eventId);
}