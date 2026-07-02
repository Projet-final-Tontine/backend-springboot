package ht.edu.ueh.fds.tontine.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Transforme les exceptions en reponses JSON coherentes pour l'API.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Regle metier violee -> 400 Bad Request avec un message clair. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Toute autre erreur inattendue -> 500. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAutre(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne : " + ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "erreur", status.getReasonPhrase(),
                "message", message == null ? "" : message
        ));
    }
}
