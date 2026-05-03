package kosukeroku.ms_account_reservation.repository;

import kosukeroku.ms_account_reservation.model.AccountStatus;
import kosukeroku.ms_account_reservation.model.enums.AccountStatusName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountStatusRepository extends JpaRepository<AccountStatus, Integer> {

    Optional<AccountStatus> findByName(AccountStatusName name);
}
