package ht.edu.ueh.fds.tontine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fon Sekou : la caisse de solidarité d'un Sol. Les membres l'alimentent, et
 * elle vient en aide à un membre frappé par un malheur (décès, maladie,
 * catastrophe) après un vote favorable du groupe. Une seule caisse par Sol.
 */
@Entity
@Table(name = "FON_SEKOU", uniqueConstraints = {
        @UniqueConstraint(name = "uq_fon_sekou_sol", columnNames = {"sol_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FonSekou {

    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    @Column(name = "sol_id", nullable = false, length = 36)
    private String solId;

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
