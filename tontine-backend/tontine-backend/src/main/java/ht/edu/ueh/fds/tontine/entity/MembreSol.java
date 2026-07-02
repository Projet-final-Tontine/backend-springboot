package ht.edu.ueh.fds.tontine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * Participation d'un utilisateur a un Sol (table de liaison).
 * Porte l'ordre de passage dans la rotation. Mappe la table {@code MEMBRE_SOL}.
 */
@Entity
@Table(name = "MEMBRE_SOL", uniqueConstraints = {
        @UniqueConstraint(name = "uq_membre_sol_user_sol", columnNames = {"user_id", "sol_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembreSol {

    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sol_id", nullable = false)
    private Sol sol;

    @CreationTimestamp
    @Column(name = "date_adhesion", nullable = false, updatable = false)
    private LocalDateTime dateAdhesion;

    /** Valeurs attendues : ACTIF, DEFAILLANT, PARTI */
    @Column(name = "statut_membre", nullable = false, length = 20)
    @Builder.Default
    private String statutMembre = "ACTIF";

    /** Position dans la rotation (qui touche la main en 1er, 2e, ...). */
    @Column(name = "ordre_passage")
    private Integer ordrePassage;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification;
}
