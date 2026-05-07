package kosukeroku.ms_account_reservation.repository;


import kosukeroku.ms_account_reservation.model.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    @EntityGraph(attributePaths = {"accounts", "accounts.status"})
    @Query("SELECT c FROM Client c WHERE c.id = :id")
    Optional<Client> findByIdWithAccounts(UUID id);

    @Query("SELECT c.id, COUNT(a) FROM Client c LEFT JOIN c.accounts a " +
            "WHERE a.status.name = kosukeroku.ms_account_reservation.model.enums.AccountStatusName.NEW " +
            "OR a.status.name = kosukeroku.ms_account_reservation.model.enums.AccountStatusName.IN_CREATION " +
            "OR a.status.name = kosukeroku.ms_account_reservation.model.enums.AccountStatusName.CREATED " +
            "AND c.id IN :clientIds GROUP BY c.id")
    List<Object[]> countActiveAccountsForClients(@Param("clientIds") List<UUID> clientIds);
}