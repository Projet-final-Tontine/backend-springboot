package ht.edu.ueh.fds.tontine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Compte utilisateur de la plateforme.
 * Un seul type de personne, differencie par le champ {@code role}
 * (MEMBRE, MANMAN_SOL, ADMIN). La Manman sol herite des actions du Membre.
 * Mappe la table SQL {@code USER}.
 */
@Entity
@Table(name = "`USER`")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Utilisateur {

    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false, length = 10)
    private String sexe;

    @Column(nullable = false, unique = true, length = 20)
    private String telephone;

    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Identifiant public unique (« @username »), à la manière de Wise/Revolut.
     * Nullable en base pour permettre l'ajout de la colonne aux comptes existants
     * (renseigné à l'inscription et modifiable dans le profil). Unicité vérifiée
     * sans tenir compte de la casse. Un index unique accélère les recherches.
     */
    @Column(unique = true, length = 20)
    private String username;

    @Column(nullable = false)
    private String adresse;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "cin_nif", nullable = false, unique = true, length = 50)
    private String cinNif;

    @Column(name = "date_naissance", nullable = false)
    private LocalDate dateNaissance;

    @Column(name = "mot_de_passe_hache", nullable = false)
    private String motDePasseHache;

    /** Valeurs attendues : EN_ATTENTE, ACTIF, BLOQUE, INACTIF */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String statut = "EN_ATTENTE";

    /** Valeurs attendues : MEMBRE, MANMAN_SOL, ADMIN */
    @Column(nullable = false, length = 20)
    private String role;

    /**
     * Méthode d'authentification du compte : {@code LOCAL} (email + mot de passe)
     * ou {@code GOOGLE}. Nullable pour ne pas casser les comptes existants
     * (une valeur nulle est traitée comme LOCAL).
     */
    @Column(name = "auth_provider", length = 20)
    private String authProvider;

    /** Identifiant Firebase (uid) pour un compte lié à Google. */
    @Column(name = "google_uid", unique = true, length = 128)
    private String googleUid;

    /** Sert a la purge automatique des comptes inactifs (> 1 an). */
    @Column(name = "derniere_connexion")
    private LocalDateTime derniereConnexion;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification;
}
