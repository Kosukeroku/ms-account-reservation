package kosukeroku.ms_account_reservation.config;

import jakarta.annotation.PostConstruct;
import kosukeroku.ms_account_reservation.model.AccountStatus;
import kosukeroku.ms_account_reservation.repository.AccountStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StatusInitializer {

    private final AccountStatusRepository repository;

    @PostConstruct
    public void init() {
        if (repository.count() == 0) {
            List<AccountStatus> statuses = Arrays.stream(AccountStatus.StatusName.values())
                    .map(AccountStatus::new)
                    .collect(Collectors.toList());
            repository.saveAll(statuses);
        }
    }
}