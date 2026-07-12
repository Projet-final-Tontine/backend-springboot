package ht.edu.ueh.fds.tontine.dto;

import java.util.List;

/** Corps de requetes pour les sondages. */
public class SondageRequests {

    /** Creer un sondage : une question et 2 a 6 options. */
    public record CreerSondageRequest(String question, List<String> options) {
    }

    /** Voter : l'index de l'option choisie. */
    public record VoterRequest(Integer optionIndex) {
    }
}
