package ht.edu.ueh.fds.tontine.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Bénéficiaire favori d'un utilisateur : permet d'effectuer des transferts plus
 * rapidement vers des contacts fréquents.
 */
@Entity
@Table(name = "FAVORI_BENEFICIAIRE",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_favori_proprietaire_beneficiaire",
                columnNames = {"proprietaire_id", "beneficiaire_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriBeneficiaire {

    @Id
    @Column(length = 36)
    private String id;

    /** Utilisateur qui enregistre le favori. */
    @Column(name = "proprietaire_id", nullable = false, length = 36)
    private String proprietaireId;

    @Column(name = "beneficiaire_id", nullable = false, length = 36)
    private String beneficiaireId;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;
}
