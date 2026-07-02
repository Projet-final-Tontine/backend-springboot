package ht.edu.ueh.fds.tontine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * Notification adressee a un utilisateur (rappels d'echeance, alertes de cycle,
 * messages de mobilisation de la Manman sol). Mappe la table {@code NOTIFICATION}.
 */
@Entity
@Table(name = "NOTIFICATION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false)
    private String sujet;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @CreationTimestamp
    @Column(name = "date_envoi", nullable = false, updatable = false)
    private LocalDateTime dateEnvoi;

    @Column(nullable = false)
    @Builder.Default
    private Boolean lue = false;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;
}
