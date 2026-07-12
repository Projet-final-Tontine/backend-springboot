package ht.edu.ueh.fds.tontine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * Un sondage / vote de groupe dans un Sol. Les options sont stockees dans un
 * seul champ texte, separees par un saut de ligne (2 a 6 options).
 * Mappe la table {@code SONDAGE}.
 */
@Entity
@Table(name = "SONDAGE", indexes = { @Index(name = "idx_sondage_sol", columnList = "sol_id") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sondage {

    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    @Column(name = "sol_id", nullable = false, length = 36)
    private String solId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "createur_user_id", nullable = false)
    private Utilisateur createur;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String question;

    /** Options separees par un saut de ligne. */
    @Column(name = "options_texte", columnDefinition = "TEXT", nullable = false)
    private String optionsTexte;

    /** OUVERT ou CLOS. */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String statut = "OUVERT";

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;
}
