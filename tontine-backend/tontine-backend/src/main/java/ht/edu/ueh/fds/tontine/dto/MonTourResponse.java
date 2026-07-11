package ht.edu.ueh.fds.tontine.dto;

import java.time.LocalDate;

/**
 * Un tour (distribution de la main) d'un des Sols de l'utilisateur connecte,
 * pour alimenter le calendrier intelligent de l'accueil.
 * `jeSuisBeneficiaire` est vrai lorsque c'est l'utilisateur qui recoit ce tour.
 */
public record MonTourResponse(
        String solId,
        String solNom,
        Integer numero,
        String beneficiaireNom,
        boolean jeSuisBeneficiaire,
        LocalDate datePrevue,
        String statut
) {
}
