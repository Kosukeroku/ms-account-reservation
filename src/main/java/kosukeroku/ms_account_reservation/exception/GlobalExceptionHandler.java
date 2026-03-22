package kosukeroku.ms_account_reservation.exception;

import kosukeroku.ms_account_reservation.dto.ErrorCode;
import kosukeroku.ms_account_reservation.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


    @ExceptionHandler(ClientAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleClientAlreadyExists(ClientAlreadyExistsException ex) {
        log.warn("Client already exists: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorCode(ErrorCode.CLIENT_ALREADY_EXISTS);
        errorResponse.setErrorDescription(ex.getMessage());
        errorResponse.setStatusCode(HttpStatus.CONFLICT.value());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleClientNotFound(ClientNotFoundException ex) {
        log.warn("Client not found: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorCode(ErrorCode.CLIENT_NOT_FOUND);
        errorResponse.setErrorDescription(ex.getMessage());
        errorResponse.setStatusCode(HttpStatus.NOT_FOUND.value());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorCode(ErrorCode.VALIDATION_ERROR);
        errorResponse.setErrorDescription(
                ex.getBindingResult().getAllErrors().get(0).getDefaultMessage()
        );
        errorResponse.setStatusCode(HttpStatus.BAD_REQUEST.value());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorCode(ErrorCode.NOT_FOUND);
        errorResponse.setErrorDescription("Resource not found.");
        errorResponse.setStatusCode(HttpStatus.NOT_FOUND.value());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not allowed: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorCode(ErrorCode.METHOD_NOT_ALLOWED);
        errorResponse.setErrorDescription(ex.getMessage());
        errorResponse.setStatusCode(HttpStatus.METHOD_NOT_ALLOWED.value());

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }
}

