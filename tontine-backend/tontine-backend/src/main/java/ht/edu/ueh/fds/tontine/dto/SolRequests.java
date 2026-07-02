package ht.edu.ueh.fds.tontine.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Requetes liees aux Sols, tours, cotisations et garantie. */
public final class SolRequests {

    private SolRequests() {
    }

    public record CreerSolRequest(
            String nom,
            String description,
            Integer nombreMaxMembres,
            BigDecimal montantCotisation,
            String frequence,
            LocalDate dateDebut
    ) {
    }

    public record RejoindreRequest(String codeInvitation) {
    }

    public record OuvrirTourRequest(LocalDate datePrevue) {
    }

    public record PayerMainRequest(String referenceMonCash) {
    }

    public record PayerCotisationRequest(String referenceMonCash) {
    }

    public record AlimenterCaisseRequest(BigDecimal montant) {
    }

    public record ActiverContrepartieRequest(String cotisationId, String motif) {
    }
}
