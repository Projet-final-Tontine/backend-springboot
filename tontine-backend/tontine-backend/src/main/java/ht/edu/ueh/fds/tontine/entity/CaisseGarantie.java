package ht.edu.ueh.fds.tontine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Caisse de garantie (contrepartie) : fonds de secours d'un Sol pouvant etre
 * injectes par l'Administrateur pour couvrir un impaye. Une caisse par Sol.
 * Mappe la table {@code CAISSE_GARANTIE}.
 */
@Entity
@Table(name = "CAISSE_GARANTIE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaisseGarantie {

    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    /** Une seule caisse par Sol. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sol_id", nullable = false, unique = true)
    private Sol sol;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal solde = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification;
}
