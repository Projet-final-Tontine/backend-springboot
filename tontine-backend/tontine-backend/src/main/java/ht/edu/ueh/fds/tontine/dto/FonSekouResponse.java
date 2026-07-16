package ht.edu.ueh.fds.tontine.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * État du Fon Sekou d'un Sol pour l'écran mobile : solde de la caisse, rôle de
 * l'utilisateur, et la liste des demandes de secours avec le décompte des votes.
 */
public record FonSekouResponse(
        BigDecimal solde,
        boolean estMamanSol,
        List<DemandeInfo> demandes
) {
    public record DemandeInfo(
            String id,
            String demandeurId,
            String demandeurNom,
            String type,
            BigDecimal montant,
            String motif,
            String justificatifUrl,
            String statut,
            long nbPour,
            long nbContre,
            Boolean monVote,      // true=pour, false=contre, null=pas encore voté
            boolean estMoi,       // la demande est-elle celle de l'utilisateur ?
            LocalDateTime dateCreation
    ) {
    }
}
