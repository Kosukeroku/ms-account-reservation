package kosukeroku.currencyclient.exception;

public class CurrencyClientException extends RuntimeException {

    public CurrencyClientException(String message) {
        super(message);
    }

    public CurrencyClientException(String message, Throwable cause) {
        super(message, cause);
    }
}