package ht.edu.ueh.fds.tontine.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Dossier de vérification d'identité (KYC) d'un utilisateur : type de pièce
 * fournie, images téléversées et statut de traitement.
 *
 * <p>Le cycle de vie reproduit un vrai KYC : SOUMIS (documents envoyés) puis
 * APPROUVE ou REJETE. En mode démonstration ({@code app.kyc.auto-approve}), la
 * soumission est approuvée automatiquement ; sinon un administrateur tranche.</p>
 */
@Entity
@Table(name = "VERIFICATION_KYC")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationKyc {

    @Id
    @Column(length = 36)
    private String id;

    /** Un dossier KYC par utilisateur (mis à jour à chaque nouvelle soumission). */
    @Column(name = "utilisateur_id", nullable = false, unique = true, length = 36)
    private String utilisateurId;

    /** Type de pièce : CARTE_IDENTITE, PASSEPORT, PERMIS. */
    @Column(name = "type_document", nullable = false, length = 20)
    private String typeDocument;

    /** URL de l'image recto (ou page unique du passeport). */
    @Column(name = "recto_url", length = 300)
    private String rectoUrl;

    /** URL de l'image verso (facultative selon la pièce). */
    @Column(name = "verso_url", length = 300)
    private String versoUrl;

    /** SOUMIS, APPROUVE ou REJETE. */
    @Column(nullable = false, length = 15)
    private String statut;

    @Column(name = "date_soumission", nullable = false)
    private LocalDateTime dateSoumission;

    @Column(name = "date_decision")
    private LocalDateTime dateDecision;
}
