package ht.edu.ueh.fds.tontine.dto;

import ht.edu.ueh.fds.tontine.entity.MembreSol;

/** Vue d'une participation a un Sol. */
public record MembreSolResponse(
        String id,
        String utilisateurId,
        String solId,
        Integer ordrePassage,
        String statutMembre
) {
    public static MembreSolResponse from(MembreSol m) {
        return new MembreSolResponse(
                m.getId(), m.getUtilisateur().getId(), m.getSol().getId(),
                m.getOrdrePassage(), m.getStatutMembre());
    }
}
