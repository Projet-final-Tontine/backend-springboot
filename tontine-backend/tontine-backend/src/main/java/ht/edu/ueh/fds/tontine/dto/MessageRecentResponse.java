package ht.edu.ueh.fds.tontine.dto;

import java.time.LocalDateTime;

/**
 * Message recent destine a l'utilisateur, pour les notifications.
 * {@code solNom} est renseigne pour un message de groupe, {@code null} pour un
 * message prive.
 */
public record MessageRecentResponse(
        String expediteurNom,
        String apercu,
        String solId,
        String solNom,
        LocalDateTime dateEnvoi
) {
}
