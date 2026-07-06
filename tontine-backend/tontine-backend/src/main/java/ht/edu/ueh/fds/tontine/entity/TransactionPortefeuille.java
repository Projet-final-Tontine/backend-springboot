package ht.edu.ueh.fds.tontine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ecriture du journal du portefeuille : chaque mouvement (depot, cotisation,
 * gain de la main, retrait) est trace ici pour garantir une tracabilite
 * complete des fonds. Mappe la table {@code TRANSACTION_PORTEFEUILLE}.
 */
@Entity
@Table(name = "TRANSACTION_PORTEFEUILLE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionPortefeuille {

    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portefeuille_id", nullable = false)
    private Portefeuille portefeuille;

    /** Valeurs attendues : DEPOT, COTISATION, GAIN_MAIN, RETRAIT. */
    @Column(nullable = false, length = 20)
    private String type;

    /** CREDIT (entree d'argent) ou DEBIT (sortie). */
    @Column(nullable = false, length = 10)
    private String sens;

    /** Montant du mouvement (toujours positif), en gourdes (HTG). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    /** Solde du portefeuille apres application du mouvement (piste d'audit). */
    @Column(name = "solde_apres", nullable = false, precision = 12, scale = 2)
    private BigDecimal soldeApres;

    /** Reference externe (ex. reference de transaction Mon Cash pour un depot). */
    @Column(name = "reference_externe")
    private String referenceExterne;

    /** Libelle lisible du mouvement (ex. « Depot Mon Cash », « Cotisation Sol X »). */
    @Column(length = 200)
    private String description;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;
}
