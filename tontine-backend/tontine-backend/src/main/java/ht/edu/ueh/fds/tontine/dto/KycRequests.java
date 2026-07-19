package ht.edu.ueh.fds.tontine.dto;

/** Requêtes et réponse de la vérification d'identité (KYC). */
public class KycRequests {

    /** Étape 1 : l'utilisateur confirme/corrige son identité. */
    public record MajIdentiteRequest(
            String nom,
            String prenom,
            String dateNaissance,   // format AAAA-MM-JJ
            String adresse) {
    }

    /** Étape finale : soumission des documents. */
    public record SoumettreKycRequest(
            String typeDocument,    // CARTE_IDENTITE, PASSEPORT, PERMIS
            String rectoUrl,
            String versoUrl) {
    }

    /**
     * État courant du KYC (identité pré-remplie + statut de vérification).
     *
     * @param statut NON_SOUMIS, SOUMIS, APPROUVE ou REJETE
     */
    public record KycEtatResponse(
            String nom,
            String prenom,
            String dateNaissance,
            String adresse,
            String statut,
            String typeDocument,
            String dateSoumission) {
    }
}
