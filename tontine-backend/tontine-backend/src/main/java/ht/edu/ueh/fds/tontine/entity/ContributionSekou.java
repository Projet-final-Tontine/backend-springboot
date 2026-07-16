package ht.edu.ueh.fds.tontine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Contribution d'un membre au Fon Sekou de son Sol (journal des versements). */
@Entity
@Table(name = "CONTRIBUTION_SEKOU",
        indexes = { @Index(name = "idx_contribution_sekou_sol", columnList = "sol_id") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContributionSekou {

    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    @Column(name = "sol_id", nullable = false, length = 36)
    private String solId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contributeur_user_id", nullable = false)
    private Utilisateur contributeur;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;
}
