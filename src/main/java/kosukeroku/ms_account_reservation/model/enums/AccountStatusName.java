package kosukeroku.ms_account_reservation.model.enums;

public enum AccountStatusName {
    NEW("Счёт создан в БД"),
    IN_CREATION("Запрос на создание счета был отправлен в смежную систему"),
    CREATED("Счёт создан в смежной системе"),
    CANCELLED("Счёт аннулирован"),
    CLOSED("Счёт закрыт");

    private final String description;

    AccountStatusName(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}