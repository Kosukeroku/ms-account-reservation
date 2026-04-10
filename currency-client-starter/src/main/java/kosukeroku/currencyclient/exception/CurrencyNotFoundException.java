package kosukeroku.currencyclient.exception;

public class CurrencyNotFoundException extends CurrencyClientException {
    public CurrencyNotFoundException(String message) {
        super(message);
    }
}