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
 * Un cercle de tontine (Sol). Cree et dirige par une Manman sol.
 * Mappe la table SQL {@code SOL}.
 */
@Entity
@Table(name = "SOL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sol {

    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Code partage permettant a un membre de rejoindre le Sol. */
    @Column(name = "code_invitation", nullable = false, unique = true, length = 20)
    private String codeInvitation;

    @Column(name = "nombre_max_membres", nullable = false)
    private Integer nombreMaxMembres;

    @Column(name = "montant_cotisation", nullable = false, precision = 10, scale = 2)
    private BigDecimal montantCotisation;

    /** Valeurs attendues : HEBDOMADAIRE, MENSUEL */
    @Column(nullable = false, length = 20)
    private String frequence;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    /** La Manman sol organisatrice du cercle. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maman_sol_id", nullable = false)
    private Utilisateur mamanSol;

    /** Valeurs attendues : OUVERT, EN_COURS, TERMINE */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String statut = "OUVERT";

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification;
}
