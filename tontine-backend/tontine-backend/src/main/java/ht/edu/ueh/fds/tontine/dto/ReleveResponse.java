package ht.edu.ueh.fds.tontine.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Relevé de Fiabilité Financière renvoyé au membre : toutes les statistiques
 * de son comportement de paiement, son score, et les éléments de vérification
 * (référence + hash + URL) à imprimer sur le PDF / le QR code.
 */
public record ReleveResponse(
        String reference,
        String nomComplet,
        LocalDate membreDepuis,
        int nbSols,
        BigDecimal totalCotise,
        int nbCotisations,
        int nbATemps,
        int nbRetards,
        int nbDefauts,
        int scoreGlobal,
        String note,          // A, B, C, D, N
        String niveau,        // libellé lisible
        boolean historiqueSuffisant,
        LocalDateTime dateEmission,
        String hash,
        String urlVerification
) {
}
