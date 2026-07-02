package ht.edu.ueh.fds.tontine.dto;

import ht.edu.ueh.fds.tontine.entity.Paiement;

import java.math.BigDecimal;

/** Vue d'un paiement (flux Mon Cash). */
public record PaiementResponse(
        String id,
        String cotisationId,
        String utilisateurId,
        String typePaiement,
        String referenceTransaction,
        BigDecimal montantPaye,
        String statutPaiement
) {
    public static PaiementResponse from(Paiement p) {
        return new PaiementResponse(
                p.getId(), p.getCotisation().getId(), p.getUtilisateur().getId(),
                p.getTypePaiement(), p.getReferenceTransaction(),
                p.getMontantPaye(), p.getStatutPaiement());
    }
}
