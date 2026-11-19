package pl.pg.pogodulka.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import lombok.extern.slf4j.Slf4j;
import pl.pg.pogodulka.dto.ErrorResponse;
import pl.pg.pogodulka.model.BadRequestException;
import pl.pg.pogodulka.model.NotFoundException;
import pl.pg.pogodulka.model.ProviderUnavailableException;

/**
 * Zamienia wyjątki na odpowiedzi {@code {"error": "..."}} zgodnie z kontraktem.
 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    static final String RETRY_AFTER_SECONDS = "60";

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> badRequest(BadRequestException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> missingParam(MissingServletRequestParameterException e) {
        return error(HttpStatus.BAD_REQUEST, "Missing parameter '%s'".formatted(e.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> typeMismatch(MethodArgumentTypeMismatchException e) {
        return error(HttpStatus.BAD_REQUEST,
                "Invalid value '%s' for parameter '%s'".formatted(e.getValue(), e.getName()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(NotFoundException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ProviderUnavailableException.class)
    public ResponseEntity<ErrorResponse> providerUnavailable(ProviderUnavailableException e) {
        log.warn("Weather provider unavailable: {}", e.getMessage(), e.getCause());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
                .body(new ErrorResponse("Weather provider unavailable, try again later"));
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(message));
    }
}
