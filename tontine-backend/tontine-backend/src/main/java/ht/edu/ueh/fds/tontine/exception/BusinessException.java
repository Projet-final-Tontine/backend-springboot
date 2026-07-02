package ht.edu.ueh.fds.tontine.exception;

/**
 * Exception metier : levee quand une regle du Sol est violee
 * (code invalide, sol plein, dette active, solde insuffisant, ...).
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
