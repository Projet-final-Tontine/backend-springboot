package ht.edu.ueh.fds.tontine.dto;

import ht.edu.ueh.fds.tontine.entity.BlocRegistre;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Représentation d'un bloc du Registre Inviolable pour l'application mobile.
 * Le hash est renvoyé en entier (preuve) ; l'écran peut n'en afficher qu'un
 * extrait lisible.
 */
public record BlocRegistreResponse(
        long position,
        LocalDateTime date,
        String type,
        String sens,
        BigDecimal montant,
        String description,
        String hash,
        String hashPrecedent) {

    public static BlocRegistreResponse from(BlocRegistre b) {
        return new BlocRegistreResponse(
                b.getPosition(), b.getDateScellement(), b.getType(), b.getSens(),
                b.getMontant(), b.getDescription(), b.getHash(), b.getHashPrecedent());
    }
}
