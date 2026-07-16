package ht.edu.ueh.fds.tontine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Certificat de Fiabilite Financiere emis pour un membre : une photographie,
 * a un instant donne, de son comportement de paiement dans les tontines.
 * Il est persiste pour permettre a un tiers (banque, microfinance) de verifier
 * son authenticite via la reference et le hash, meme des mois plus tard.
 */
@Entity
@Table(name = "CERTIFICAT_FIABILITE", uniqueConstraints = {
        @UniqueConstraint(name = "uq_certificat_reference", columnNames = {"reference"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificatFiabilite {

    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    /** Reference publique unique (ex : SOL-7K3M9QP2), imprimee sur le releve. */
    @Column(nullable = false, length = 20)
    private String reference;

    @Column(name = "user_id", nullable = false, length = 36)
    private String utilisateurId;

    /** Copie du nom au moment de l'emission (le releve ne change plus apres). */
    @Column(name = "nom_complet", nullable = false, length = 200)
    private String nomComplet;

    @Column(name = "membre_depuis")
    private LocalDate membreDepuis;

    @Column(name = "nb_sols", nullable = false)
    private int nbSols;

    @Column(name = "total_cotise", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalCotise;

    @Column(name = "nb_cotisations", nullable = false)
    private int nbCotisations;

    @Column(name = "nb_a_temps", nullable = false)
    private int nbATemps;

    @Column(name = "nb_retards", nullable = false)
    private int nbRetards;

    @Column(name = "nb_defauts", nullable = false)
    private int nbDefauts;

    @Column(name = "score_global", nullable = false)
    private int scoreGlobal;

    /** Note lisible : A, B, C, D, ou N (nouveau / historique insuffisant). */
    @Column(nullable = false, length = 2)
    private String note;

    /** Empreinte anti-falsification (SHA-256 du contenu + secret serveur). */
    @Column(nullable = false, length = 64)
    private String hash;

    @CreationTimestamp
    @Column(name = "date_emission", nullable = false, updatable = false)
    private LocalDateTime dateEmission;
}
