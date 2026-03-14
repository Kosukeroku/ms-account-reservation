package kosukeroku.ms_account_reservation.model;

import jakarta.persistence.*;
import kosukeroku.ms_account_reservation.model.enums.AccountStatusName;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "account_status")
@Data
@NoArgsConstructor
public class AccountStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false)
    private AccountStatusName name;

    @Column(name = "description")
    private String description;

    public AccountStatus(AccountStatusName name) {
        this.name = name;
        this.description = name.getDescription();
    }
}