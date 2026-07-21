package ht.edu.ueh.fds.tontine.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transfert d'argent entre deux utilisateurs. Un seul enregistrement décrit
 * l'opération vue des deux côtés (expéditeur + bénéficiaire) : l'historique
 * d'un utilisateur regroupe les transferts où il est l'un ou l'autre.
 *
 * <p>Chaque transfert porte un numéro de confirmation lisible unique
 * ({@code reference}) et un identifiant technique ({@code transactionId}),
 * comme un reçu bancaire. L'opération est par ailleurs scellée dans le Registre
 * Inviolable via le portefeuille.</p>
 */
@Entity
@Table(name = "TRANSFERT",
        indexes = {
                @Index(name = "idx_transfert_expediteur", columnList = "expediteur_id"),
                @Index(name = "idx_transfert_beneficiaire", columnList = "beneficiaire_id"),
                @Index(name = "idx_transfert_reference", columnList = "reference", unique = true),
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transfert {

    @Id
    @Column(length = 36)
    private String id;

    /** Numéro de confirmation lisible et unique (ex. SOL-20260720-483921). */
    @Column(nullable = false, unique = true, length = 30)
    private String reference;

    /** Identifiant technique de transaction (ex. TX-9F3A1C7B2D). */
    @Column(name = "transaction_id", nullable = false, length = 30)
    private String transactionId;

    @Column(name = "expediteur_id", nullable = false, length = 36)
    private String expediteurId;
    @Column(name = "expediteur_username", length = 20)
    private String expediteurUsername;
    @Column(name = "expediteur_nom", length = 120)
    private String expediteurNom;

    @Column(name = "beneficiaire_id", nullable = false, length = 36)
    private String beneficiaireId;
    @Column(name = "beneficiaire_username", length = 20)
    private String beneficiaireUsername;
    @Column(name = "beneficiaire_nom", length = 120)
    private String beneficiaireNom;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    /** Devise (HTG par défaut ; prévu pour une évolution multidevise). */
    @Column(nullable = false, length = 5)
    private String devise;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal frais;

    @Column(length = 200)
    private String message;

    /** REUSSI, EN_ATTENTE, ECHEC ou ANNULE. */
    @Column(nullable = false, length = 15)
    private String statut;

    /** Méthode d'authentification utilisée (ex. « Empreinte digitale »). */
    @Column(name = "methode_auth", length = 40)
    private String methodeAuth;

    /** Solde de l'expéditeur après le transfert (affiché sur le reçu). */
    @Column(name = "solde_apres_expediteur", precision = 12, scale = 2)
    private BigDecimal soldeApresExpediteur;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;
}
