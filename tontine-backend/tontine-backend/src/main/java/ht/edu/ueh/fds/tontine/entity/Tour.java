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
 * Une echeance du cycle : le pot (la « main ») est attribue a un beneficiaire.
 * Mappe la table {@code TOUR}.
 */
@Entity
@Table(name = "TOUR", uniqueConstraints = {
        @UniqueConstraint(name = "uq_tour_sol_numero", columnNames = {"sol_id", "numero_tour"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tour {

    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sol_id", nullable = false)
    private Sol sol;

    /** Le membre qui touche la main lors de ce tour. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beneficiaire_user_id", nullable = false)
    private Utilisateur beneficiaire;

    @Column(name = "numero_tour", nullable = false)
    private Integer numeroTour;

    @Column(name = "date_prevue", nullable = false)
    private LocalDate datePrevue;

    @Column(name = "date_effective_distribution")
    private LocalDateTime dateEffectiveDistribution;

    @Column(name = "montant_pot_distribue", precision = 10, scale = 2)
    private BigDecimal montantPotDistribue;

    /** Valeurs attendues : EN_ATTENTE, OUVERT, CLOTURE */
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
