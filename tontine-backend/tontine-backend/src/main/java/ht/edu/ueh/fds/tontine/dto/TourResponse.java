package ht.edu.ueh.fds.tontine.dto;

import ht.edu.ueh.fds.tontine.entity.Tour;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Vue d'un tour de table. */
public record TourResponse(
        String id,
        String solId,
        String beneficiaireId,
        Integer numeroTour,
        LocalDate datePrevue,
        String statut,
        BigDecimal montantPotDistribue
) {
    public static TourResponse from(Tour t) {
        return new TourResponse(
                t.getId(), t.getSol().getId(), t.getBeneficiaire().getId(),
                t.getNumeroTour(), t.getDatePrevue(), t.getStatut(), t.getMontantPotDistribue());
    }
}
