package kosukeroku.ms_account_reservation.repository;


import kosukeroku.ms_account_reservation.model.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {

    boolean existsByMdmId(Long mdmId);

    Optional<Client> findByMdmId(Long mdmId);

    Page<Client> findByLastNameContainingIgnoreCase(String lastName, Pageable pageable);

    Page<Client> findByMdmId(Long mdmId, Pageable pageable);

    Page<Client> findByLastNameContainingIgnoreCaseAndMdmId(String lastName, Long mdmId, Pageable pageable);

    Optional<Client> findById(UUID id);
}