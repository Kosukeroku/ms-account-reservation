package kosukeroku.ms_account_reservation.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    CLIENT_NOT_FOUND("Клиент не найден", 404),
    CLIENT_ALREADY_EXISTS("Клиент с таким mdmId уже существует", 409),
    CLIENT_HAS_ACCOUNTS("У клиента есть активные счета", 409),
    VALIDATION_ERROR("Ошибка валидации", 400),
    UNAUTHORIZED("Неавторизован", 401),
    FORBIDDEN("Доступ запрещен", 403),
    NOT_FOUND("Ресурс не найден", 404),
    METHOD_NOT_ALLOWED("Метод не поддерживается", 405),
    INTERNAL_ERROR("Внутренняя ошибка сервера", 500),
    SERVICE_UNAVAILABLE("Сервис недоступен", 503),
    GATEWAY_TIMEOUT("Нет ответа", 504);

    private final String description;
    private final int statusCode;
}