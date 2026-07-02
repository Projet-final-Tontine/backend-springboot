package ht.edu.ueh.fds.tontine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Versement attendu d'un membre pour un tour donne.
 * Mappe la table {@code COTISATION}.
 */
@Entity
@Table(name = "COTISATION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cotisation {

    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_sol_id", nullable = false)
    private MembreSol membreSol;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sol_id", nullable = false)
    private Sol sol;

    /** Le tour precis auquel se rattache cette cotisation (peut etre null). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id")
    private Tour tour;

    @Column(name = "montant_attendu", nullable = false, precision = 10, scale = 2)
    private BigDecimal montantAttendu;

    @Column(name = "montant_paye", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal montantPaye = BigDecimal.ZERO;

    @Column(name = "date_echeance", nullable = false)
    private LocalDate dateEcheance;

    @Column(name = "date_paiement_effectif")
    private LocalDateTime datePaiementEffectif;

    /** Valeurs attendues : EN_ATTENTE, VALIDE, REJETE */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String statut = "EN_ATTENTE";

    @Column(name = "penalite_appliquee", nullable = false)
    @Builder.Default
    private Boolean penaliteAppliquee = false;

    @Column(name = "montant_penalite", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal montantPenalite = BigDecimal.ZERO;

    @Column(name = "recu_pdf_url")
    private String recuPdfUrl;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification;
}
