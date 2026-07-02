package ht.edu.ueh.fds.tontine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Flux financier concret (via Mon Cash) rattache a une cotisation.
 * Valide par la Manman sol. Mappe la table {@code PAIEMENT}.
 */
@Entity
@Table(name = "PAIEMENT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Paiement {

    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cotisation_id", nullable = false)
    private Cotisation cotisation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Utilisateur utilisateur;

    /** Valeurs attendues : COTISATION, VERSEMENT_MAIN, GARANTIE */
    @Column(name = "type_paiement", nullable = false, length = 50)
    private String typePaiement;

    /** Reference de transaction Mon Cash. */
    @Column(name = "reference_transaction")
    private String referenceTransaction;

    @Column(name = "montant_paye", nullable = false, precision = 10, scale = 2)
    private BigDecimal montantPaye;

    @CreationTimestamp
    @Column(name = "date_paiement", nullable = false, updatable = false)
    private LocalDateTime datePaiement;

    /** Valeurs attendues : EN_ATTENTE, SUCCES, ECHEC */
    @Column(name = "statut_paiement", nullable = false, length = 20)
    @Builder.Default
    private String statutPaiement = "EN_ATTENTE";

    /** L'utilisateur (Manman sol) qui a valide le paiement. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par_user_id")
    private Utilisateur validePar;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification;
}
