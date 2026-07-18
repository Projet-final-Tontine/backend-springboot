package ht.edu.ueh.fds.tontine.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Paiement traité par la passerelle de l'application (Mon Cash, NatCash, carte
 * Visa/Mastercard, virement). Reproduit le cycle de vie d'un vrai paiement en
 * ligne : un ordre est d'abord créé (EN_ATTENTE) avec un identifiant unique,
 * l'utilisateur paie sur la page de la passerelle, puis l'ordre est confirmé
 * (PAYE) — ce qui crédite ou débite le portefeuille et scelle un bloc au
 * Registre Inviolable.
 *
 * <p>En l'état, la confirmation est simulée (aucun compte marchand requis). Le
 * jour d'un vrai branchement (API Digicel Mon Cash, etc.), seule l'étape de
 * confirmation change : le reste du flux reste identique.</p>
 */
@Entity
@Table(name = "PAIEMENT_SIMULE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaiementSimule {

    /** Identifiant de l'ordre de paiement (sert aussi de jeton dans l'URL). */
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "utilisateur_id", nullable = false, length = 36)
    private String utilisateurId;

    /** Sens de l'opération : DEPOT (entrée) ou RETRAIT (sortie). */
    @Column(nullable = false, length = 10)
    private String sens;

    /** Moyen choisi : MONCASH, NATCASH, VISA, MASTERCARD, VIREMENT. */
    @Column(nullable = false, length = 20)
    private String moyen;

    /** Montant de l'opération, en gourdes (HTG). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    /** EN_ATTENTE, PAYE ou ECHOUE. */
    @Column(nullable = false, length = 15)
    private String statut;

    /** Référence lisible affichée à l'utilisateur (ex. « SOL-PAY-XXXX »). */
    @Column(nullable = false, length = 40)
    private String reference;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_confirmation")
    private LocalDateTime dateConfirmation;
}
