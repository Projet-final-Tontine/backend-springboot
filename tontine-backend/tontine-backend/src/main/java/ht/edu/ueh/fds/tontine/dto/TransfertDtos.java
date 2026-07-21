package ht.edu.ueh.fds.tontine.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Requêtes et réponses de la fonctionnalité « Transférer de l'argent ». */
public class TransfertDtos {

    /** Demande de transfert (après confirmation + authentification côté app). */
    public record TransfertRequest(
            String beneficiaire,   // @username ou e-mail
            BigDecimal montant,
            String devise,         // HTG par défaut
            String message,        // facultatif
            String methodeAuth) {  // ex. « Empreinte digitale »
    }

    /** Reçu numérique renvoyé après un transfert réussi. */
    public record RecuTransfertResponse(
            String id,
            String reference,        // numéro de confirmation
            String transactionId,
            String statut,
            BigDecimal montant,
            String devise,
            BigDecimal frais,
            BigDecimal totalDebite,
            String message,
            LocalDateTime date,
            String expediteurNom,
            String expediteurUsername,
            String beneficiaireNom,
            String beneficiaireUsername,
            BigDecimal soldeRestant,
            String urlVerification) { // encodée dans le QR code
    }

    /** Élément d'historique (vu du côté de l'utilisateur courant). */
    public record TransfertHistoriqueItem(
            String id,
            String sens,             // ENVOYE ou RECU
            String autreNom,         // l'autre partie (bénéficiaire ou expéditeur)
            String autreUsername,
            String autrePhotoUrl,
            BigDecimal montant,
            String devise,
            String statut,
            LocalDateTime date,
            String reference,
            String transactionId) {
    }

    /** Détail complet d'un transfert. */
    public record TransfertDetailResponse(
            String id,
            String sens,
            String statut,
            BigDecimal montant,
            String devise,
            BigDecimal frais,
            String expediteurNom,
            String expediteurUsername,
            String beneficiaireNom,
            String beneficiaireUsername,
            LocalDateTime date,
            String reference,
            String transactionId,
            String message,
            String methodeAuth) {
    }

    /** Bénéficiaire favori. */
    public record FavoriResponse(
            String id,
            String beneficiaireId,
            String username,
            String nomComplet,
            String photoUrl,
            boolean kycVerifie) {
    }
}
