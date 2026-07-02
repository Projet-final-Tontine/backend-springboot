package ht.edu.ueh.fds.tontine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Trace un declenchement de la contrepartie par l'Administrateur pour couvrir
 * la main d'un tour en cas de defaillance. Mappe la table {@code ACTIVATION_GARANTIE}.
 */
@Entity
@Table(name = "ACTIVATION_GARANTIE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivationGarantie {

    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caisse_garantie_id", nullable = false)
    private CaisseGarantie caisseGarantie;

    /** Le tour sauve par l'injection des fonds. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    /** Le membre en defaut de paiement. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_defaillant_id", nullable = false)
    private MembreSol membreDefaillant;

    /** L'administrateur qui a declenche le secours. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    private Utilisateur admin;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montant;

    @Column(columnDefinition = "TEXT")
    private String motif;

    /** Le membre defaillant a-t-il rembourse la caisse ? */
    @Column(nullable = false)
    @Builder.Default
    private Boolean rembourse = false;

    @CreationTimestamp
    @Column(name = "date_activation", nullable = false, updatable = false)
    private LocalDateTime dateActivation;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification;
}
