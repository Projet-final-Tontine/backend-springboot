package ht.edu.ueh.fds.tontine.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bloc du « Registre Inviolable » — le grand livre de comptes infalsifiable.
 *
 * Chaque mouvement d'argent de la plateforme (dépôt, cotisation, gain de la
 * main, Fon Sekou, garantie…) est scellé dans un bloc relié au précédent par
 * son empreinte ({@code hashPrecedent}). Le bloc calcule ensuite son propre
 * {@code hash} = SHA-256(contenu essentiel + hash du bloc précédent + secret
 * serveur).
 *
 * Conséquence : modifier ne serait-ce qu'un chiffre d'un ancien bloc invalide
 * son hash ET, en cascade, tous les blocs suivants. La fraude devient
 * mathématiquement détectable — et impossible à « recoudre » sans le secret
 * serveur (que le fraudeur n'a pas). Mappe la table {@code BLOC_REGISTRE}.
 */
@Entity
@Table(name = "BLOC_REGISTRE",
        indexes = @Index(name = "idx_bloc_position", columnList = "position", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlocRegistre {

    @Id
    @Column(length = 36)
    private String id;

    /** Rang du bloc dans la chaîne (0 = bloc de genèse). Ordre = intégrité. */
    @Column(name = "position", nullable = false, unique = true)
    private long position;

    /** Horodatage du scellement (figé : entre dans le calcul du hash). */
    @Column(name = "date_scellement", nullable = false, updatable = false)
    private LocalDateTime dateScellement;

    /** Type de mouvement : DEPOT, COTISATION, GAIN_MAIN, FON_SEKOU… */
    @Column(nullable = false, length = 30)
    private String type;

    /** CREDIT (entrée d'argent) ou DEBIT (sortie). */
    @Column(nullable = false, length = 10)
    private String sens;

    /** Montant du mouvement (toujours positif), en gourdes (HTG). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    /** Identifiant de l'utilisateur concerné par le mouvement (piste d'audit). */
    @Column(name = "utilisateur_id", length = 36)
    private String utilisateurId;

    /** Libellé lisible du mouvement. */
    @Column(length = 200)
    private String description;

    /** Lien vers l'écriture du journal du portefeuille correspondante. */
    @Column(name = "transaction_id", length = 36)
    private String transactionId;

    /** Empreinte du bloc précédent (chaînage). Bloc de genèse : 64 zéros. */
    @Column(name = "hash_precedent", nullable = false, length = 64)
    private String hashPrecedent;

    /** Empreinte SHA-256 de ce bloc (sceau infalsifiable). */
    @Column(nullable = false, length = 64)
    private String hash;
}
