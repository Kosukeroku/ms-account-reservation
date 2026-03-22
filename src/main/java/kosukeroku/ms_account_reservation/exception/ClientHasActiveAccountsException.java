package kosukeroku.ms_account_reservation.exception;

public class ClientHasActiveAccountsException extends RuntimeException {
    public ClientHasActiveAccountsException(String message) {
        super(message);
    }
}
