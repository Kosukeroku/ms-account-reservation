package kosukeroku.ms_account_reservation.repository;

import kosukeroku.ms_account_reservation.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

}
