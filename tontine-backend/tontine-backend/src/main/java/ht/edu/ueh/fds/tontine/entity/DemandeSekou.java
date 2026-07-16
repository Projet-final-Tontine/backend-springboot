package ht.edu.ueh.fds.tontine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Demande de secours d'un membre au Fon Sekou de son Sol.
 * Le groupe vote ; la Manman sol clôture. Si le vote est favorable et la caisse
 * suffisante, le montant est versé dans le portefeuille du demandeur.
 */
@Entity
@Table(name = "DEMANDE_SEKOU",
        indexes = { @Index(name = "idx_demande_sekou_sol", columnList = "sol_id") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeSekou {

    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    @Column(name = "sol_id", nullable = false, length = 36)
    private String solId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demandeur_user_id", nullable = false)
    private Utilisateur demandeur;

    /** DECES, MALADIE, CATASTROPHE, AUTRE. */
    @Column(nullable = false, length = 20)
    private String type;

    @Column(name = "montant_demande", nullable = false, precision = 12, scale = 2)
    private BigDecimal montantDemande;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String motif;

    /** URL d'une photo justificative (facultative). */
    @Column(name = "justificatif_url")
    private String justificatifUrl;

    /** EN_ATTENTE, PAYE (approuvé et versé), REJETE. */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String statut = "EN_ATTENTE";

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification;
}
