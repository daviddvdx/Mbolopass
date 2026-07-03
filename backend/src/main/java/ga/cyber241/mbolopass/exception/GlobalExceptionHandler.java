package ga.cyber241.mbolopass.exception;

import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(ApiException.class)
  ResponseEntity<Map<String, Object>> api(ApiException ex) {
    return error(ex.status(), ex.getMessage());
  }

  @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
  ResponseEntity<Map<String, Object>> validation(Exception ex) {
    return error(HttpStatus.BAD_REQUEST, "Requete invalide");
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<Map<String, Object>> denied() {
    return error(HttpStatus.FORBIDDEN, "Acces refuse");
  }

  @ExceptionHandler(AuthenticationException.class)
  ResponseEntity<Map<String, Object>> auth() {
    return error(HttpStatus.UNAUTHORIZED, "Identifiants invalides");
  }

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<Map<String, Object>> responseStatus(ResponseStatusException ex) {
    return error(HttpStatus.valueOf(ex.getStatusCode().value()), ex.getReason() == null ? "Requete refusee" : ex.getReason());
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<Map<String, Object>> generic() {
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne");
  }

  private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(Map.of("timestamp", Instant.now(), "status", status.value(), "message", message));
  }
}
