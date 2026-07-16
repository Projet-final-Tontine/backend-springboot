package ht.edu.ueh.fds.tontine.dto;

import java.time.LocalDateTime;

/**
 * Réponse de vérification d'un relevé (endpoint public consulté par une banque).
 * {@code authentique} est faux si la référence est introuvable.
 */
public record VerificationReleveResponse(
        boolean authentique,
        String nomComplet,
        int scoreGlobal,
        String note,
        String niveau,
        LocalDateTime dateEmission,
        String message
) {
}
