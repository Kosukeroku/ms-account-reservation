package kosukeroku.ms_account_reservation.repository;

import kosukeroku.ms_account_reservation.model.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountStatusRepository extends JpaRepository<AccountStatus, Integer> {


}
