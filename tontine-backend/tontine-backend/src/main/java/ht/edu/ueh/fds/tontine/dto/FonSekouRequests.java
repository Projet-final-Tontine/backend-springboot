package ht.edu.ueh.fds.tontine.dto;

import java.math.BigDecimal;

/** Requêtes du Fon Sekou : contribuer, demander un secours, voter. */
public class FonSekouRequests {

    public record ContribuerRequest(BigDecimal montant) {
    }

    public record DemanderSekouRequest(
            String type,
            BigDecimal montant,
            String motif,
            String justificatifUrl) {
    }

    public record VoterSekouRequest(boolean pour) {
    }
}
