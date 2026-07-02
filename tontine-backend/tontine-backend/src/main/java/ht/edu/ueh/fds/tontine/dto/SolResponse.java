package ht.edu.ueh.fds.tontine.dto;

import ht.edu.ueh.fds.tontine.entity.Sol;

import java.math.BigDecimal;

/** Vue d'un cercle de tontine. */
public record SolResponse(
        String id,
        String nom,
        String description,
        String codeInvitation,
        Integer nombreMaxMembres,
        BigDecimal montantCotisation,
        String frequence,
        String statut,
        String mamanSolId
) {
    public static SolResponse from(Sol s) {
        return new SolResponse(
                s.getId(), s.getNom(), s.getDescription(), s.getCodeInvitation(),
                s.getNombreMaxMembres(), s.getMontantCotisation(), s.getFrequence(),
                s.getStatut(), s.getMamanSol().getId());
    }
}
