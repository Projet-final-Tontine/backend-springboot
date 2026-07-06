package ht.edu.ueh.fds.tontine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Portefeuille (wallet) personnel d'un utilisateur.
 *
 * La plateforme centralise les fonds : chaque membre alimente son solde par des
 * depots (Mon Cash), et toutes les cotisations sont prelevees uniquement sur ce
 * solde. La Manman sol ne recoit jamais d'argent directement d'un membre.
 * Mappe la table {@code PORTEFEUILLE}.
 */
@Entity
@Table(name = "PORTEFEUILLE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Portefeuille {

    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    /** Le proprietaire du portefeuille (un seul portefeuille par utilisateur). */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private Utilisateur utilisateur;

    /** Solde disponible, en gourdes (HTG). Toujours >= 0. */
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal solde = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification;
}
