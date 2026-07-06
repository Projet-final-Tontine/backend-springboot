package ht.edu.ueh.fds.tontine.dto;

import ht.edu.ueh.fds.tontine.entity.TransactionPortefeuille;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Vue d'un mouvement du portefeuille (journal). */
public record TransactionPortefeuilleResponse(
        String id,
        String type,
        String sens,
        BigDecimal montant,
        BigDecimal soldeApres,
        String referenceExterne,
        String description,
        LocalDateTime dateCreation
) {
    public static TransactionPortefeuilleResponse from(TransactionPortefeuille t) {
        return new TransactionPortefeuilleResponse(
                t.getId(), t.getType(), t.getSens(), t.getMontant(),
                t.getSoldeApres(), t.getReferenceExterne(), t.getDescription(),
                t.getDateCreation());
    }
}
