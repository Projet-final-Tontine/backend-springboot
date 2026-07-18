package ht.edu.ueh.fds.tontine.dto;

import java.math.BigDecimal;

/** Requêtes et réponses de la passerelle de paiement. */
public class PaiementRequests {

    /**
     * Demande d'initialisation d'un paiement.
     *
     * @param sens    DEPOT ou RETRAIT
     * @param moyen   MONCASH, NATCASH, VISA, MASTERCARD, VIREMENT
     * @param montant montant en gourdes (HTG)
     */
    public record InitierPaiementRequest(
            String sens,
            String moyen,
            BigDecimal montant) {
    }

    /**
     * Réponse à l'initialisation : l'application ouvre {@code redirectUrl} dans
     * le navigateur pour que l'utilisateur finalise le paiement.
     */
    public record InitierPaiementResponse(
            String orderId,
            String reference,
            String redirectUrl,
            String statut) {
    }

    /** État d'un ordre de paiement (interrogé par l'application au retour). */
    public record StatutPaiementResponse(
            String orderId,
            String sens,
            String moyen,
            BigDecimal montant,
            String statut,
            String reference) {
    }
}
