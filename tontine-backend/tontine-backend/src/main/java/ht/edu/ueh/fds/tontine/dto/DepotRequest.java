package ht.edu.ueh.fds.tontine.dto;

import java.math.BigDecimal;

/**
 * Demande de depot sur le portefeuille.
 *
 * {@code referenceMonCash} est le justificatif de la transaction. Aujourd'hui
 * la confirmation est simulee ; demain, l'integration de l'API marchande Mon
 * Cash validera automatiquement ce depot.
 */
public record DepotRequest(
        BigDecimal montant,
        String referenceMonCash
) {
}
