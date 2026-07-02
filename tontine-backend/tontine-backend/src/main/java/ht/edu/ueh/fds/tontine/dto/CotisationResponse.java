package ht.edu.ueh.fds.tontine.dto;

import ht.edu.ueh.fds.tontine.entity.Cotisation;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Vue d'une cotisation. */
public record CotisationResponse(
        String id,
        String membreSolId,
        String solId,
        String tourId,
        BigDecimal montantAttendu,
        BigDecimal montantPaye,
        LocalDate dateEcheance,
        String statut
) {
    public static CotisationResponse from(Cotisation c) {
        return new CotisationResponse(
                c.getId(), c.getMembreSol().getId(), c.getSol().getId(),
                c.getTour() != null ? c.getTour().getId() : null,
                c.getMontantAttendu(), c.getMontantPaye(), c.getDateEcheance(), c.getStatut());
    }
}
