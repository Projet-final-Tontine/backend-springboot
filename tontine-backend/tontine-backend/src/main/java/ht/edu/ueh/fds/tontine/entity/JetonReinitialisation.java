package ht.edu.ueh.fds.tontine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * Jeton de reinitialisation de mot de passe (envoye par SMS).
 * Mappe la table {@code JETON_REINITIALISATION}.
 */
@Entity
@Table(name = "JETON_REINITIALISATION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JetonReinitialisation {

    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Utilisateur utilisateur;

    /** Code / lien envoye par SMS. */
    @Column(nullable = false)
    private String code;

    @Column(name = "date_expiration", nullable = false)
    private LocalDateTime dateExpiration;

    @Column(nullable = false)
    @Builder.Default
    private Boolean utilise = false;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;
}
