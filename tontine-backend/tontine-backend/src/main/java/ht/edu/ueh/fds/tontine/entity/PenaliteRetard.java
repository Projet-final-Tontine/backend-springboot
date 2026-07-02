package ht.edu.ueh.fds.tontine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Penalite appliquee a une cotisation en retard. Mappe la table {@code PENALITE_RETARD}.
 */
@Entity
@Table(name = "PENALITE_RETARD")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PenaliteRetard {

    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cotisation_id", nullable = false)
    private Cotisation cotisation;

    @Column(name = "montant_penalite", nullable = false, precision = 10, scale = 2)
    private BigDecimal montantPenalite;

    @CreationTimestamp
    @Column(name = "date_application", nullable = false, updatable = false)
    private LocalDateTime dateApplication;

    @Column(columnDefinition = "TEXT")
    private String raison;

    @Column(nullable = false)
    @Builder.Default
    private Boolean payee = false;

    @Column(name = "date_paiement_penalite")
    private LocalDateTime datePaiementPenalite;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification;
}
