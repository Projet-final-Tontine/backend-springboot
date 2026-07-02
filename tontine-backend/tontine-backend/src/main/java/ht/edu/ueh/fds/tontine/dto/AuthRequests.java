package ht.edu.ueh.fds.tontine.dto;

import java.time.LocalDate;

/** Regroupe les requetes liees aux comptes et a l'authentification. */
public final class AuthRequests {

    private AuthRequests() {
    }

    public record InscriptionRequest(
            String nom,
            String prenom,
            String sexe,
            String telephone,
            String email,
            String adresse,
            String cinNif,
            LocalDate dateNaissance,
            String motDePasse,
            String role
    ) {
    }

    public record ConnexionRequest(String telephone, String motDePasse) {
    }

    public record ModifierProfilRequest(String nom, String prenom, String adresse, String photoUrl) {
    }

    public record DemandeResetRequest(String telephone) {
    }

    public record ResetRequest(String code, String nouveauMotDePasse) {
    }
}
