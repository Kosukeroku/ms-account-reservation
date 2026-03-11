package kosukeroku.ms_account_reservation.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "account_status")
@Data
@NoArgsConstructor
public class AccountStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    private StatusName name;

    private String description;

    public AccountStatus(StatusName name) {
        this.name = name;
        this.description = name.getDescription();
    }

    @Getter
    public enum StatusName {
        NEW("Счёт создан в БД"),
        IN_CREATION("Запрос на создание счета был отправлен в смежную систему"),
        CREATED("Счёт создан в смежной системе"),
        CANCELLED("Счёт аннулирован"),
        CLOSED("Счёт закрыт");

        private final String description;

        StatusName(String description) {
            this.description = description;
        }

    }
}