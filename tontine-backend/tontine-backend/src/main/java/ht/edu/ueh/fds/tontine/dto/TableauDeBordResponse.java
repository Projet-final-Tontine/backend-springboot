package ht.edu.ueh.fds.tontine.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Vue consolidée « Mon activité » pour l'écran d'accueil : indicateurs clés,
 * projections (prochaine échéance à payer, prochaine « main » à recevoir) et
 * courbe d'épargne dans le temps. Toutes les valeurs sont calculées côté
 * serveur à partir des cotisations et des tours de l'utilisateur.
 */
public record TableauDeBordResponse(
        BigDecimal soldePortefeuille,
        BigDecimal totalCotise,
        BigDecimal totalARecevoir,
        int nbSolsActifs,
        Echeance prochaineEcheance,   // null si aucune cotisation à payer
        Main prochaineMain,           // null si aucune main à venir
        List<PointEpargne> epargne) {

    /** Prochaine cotisation à régler. */
    public record Echeance(LocalDate date, BigDecimal montant, String solNom) {
    }

    /** Prochaine distribution où l'utilisateur est bénéficiaire. */
    public record Main(LocalDate date, BigDecimal montant, String solNom) {
    }

    /** Point de la courbe d'épargne : montant cumulé cotisé à une date. */
    public record PointEpargne(LocalDate date, BigDecimal cumul) {
    }
}
