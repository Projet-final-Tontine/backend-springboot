package ht.edu.ueh.fds.tontine.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Vue d'un sondage avec ses resultats et le vote de l'utilisateur connecte. */
public record SondageResponse(
        String id,
        String question,
        String createurNom,
        String statut,
        LocalDateTime dateCreation,
        int totalVotes,
        Integer monVoteIndex,
        boolean peutCloturer,
        List<OptionResultat> options
) {
    /** Une option avec son nombre de votes et son pourcentage. */
    public record OptionResultat(int index, String texte, int votes, int pourcentage) {
    }
}
