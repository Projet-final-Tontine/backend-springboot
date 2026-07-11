package ht.edu.ueh.fds.tontine.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Vue complete d'un Sol pour l'ecran de detail :
 * informations du cercle, progression du cycle, tour en cours,
 * etat des cotisations de chaque membre et position de l'utilisateur.
 */
public record SolDetailResponse(
        SolResponse sol,
        int nombreMembres,
        int toursJoues,
        int totalTours,
        TourInfo tourCourant,
        List<TourInfo> tours,
        List<EtatCotisation> etatCotisations,
        List<MembreInfo> membres,
        PositionMembre maPosition,
        SanteSol sante,
        List<EvenementJournal> journal
) {

    /**
     * Sante financiere du Sol : pourcentage de cotisations reglees a temps
     * parmi celles arrivees a echeance. EXCELLENT >= 80, MOYEN >= 50, sinon RISQUE.
     */
    public record SanteSol(int score, String niveau) {
    }

    /**
     * Evenement du journal automatique du Sol, deduit des donnees existantes.
     * Types : ADHESION, PAIEMENT, MAIN, TOUR_OUVERT, RETARD.
     */
    public record EvenementJournal(
            java.time.LocalDateTime date,
            String type,
            String acteurNom,
            java.math.BigDecimal montant
    ) {
    }

    /** Un tour du cycle, avec le nom du beneficiaire (lisible directement). */
    public record TourInfo(
            String id,
            Integer numero,
            String beneficiaireId,
            String beneficiaireNom,
            LocalDate datePrevue,
            String statut,
            BigDecimal montantPot
    ) {
    }

    /** Etat de la cotisation d'un membre pour le tour en cours. */
    public record EtatCotisation(
            String utilisateurId,
            String membreNom,
            String photoUrl,
            Integer ordre,
            BigDecimal montant,
            String statut,
            LocalDate dateEcheance
    ) {
    }

    /** Un membre de la rotation (pour l'affichage avec avatar). */
    public record MembreInfo(
            String utilisateurId,
            String nom,
            String photoUrl,
            Integer ordre,
            String statutMembre
    ) {
    }

    /** Place de l'utilisateur connecte dans la rotation. */
    public record PositionMembre(
            int ordre,
            int total,
            LocalDate datePrevue,
            boolean dateEstimee
    ) {
    }
}
