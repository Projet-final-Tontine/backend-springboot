package ht.edu.ueh.fds.tontine.dto;

/** Requêtes et réponses de l'annuaire public (username, recherche). */
public class AnnuaireDtos {

    /** Disponibilité d'un username lors de l'inscription ou de la modification. */
    public record DisponibiliteResponse(boolean disponible, String message) {
    }

    /** Modification du username de l'utilisateur connecté. */
    public record MajUsernameRequest(String username) {
    }

    /**
     * Résultat de recherche d'un bénéficiaire (par username ou e-mail) : de quoi
     * confirmer visuellement le destinataire avant un transfert.
     */
    public record RechercheUtilisateurResponse(
            String id,
            String username,
            String nomComplet,
            String photoUrl,
            boolean kycVerifie) {
    }
}
