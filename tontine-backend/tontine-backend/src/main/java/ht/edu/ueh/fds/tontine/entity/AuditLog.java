package ht.edu.ueh.fds.tontine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * Journal des actions (tracabilite / audit), y compris les operations
 * automatiques du Systeme. Mappe la table {@code AUDIT_LOG}.
 */
@Entity
@Table(name = "AUDIT_LOG")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    /** Auteur de l'action (peut etre null pour les actions du Systeme). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Utilisateur utilisateur;

    @Column(nullable = false)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String details;

    @CreationTimestamp
    @Column(name = "date_action", nullable = false, updatable = false)
    private LocalDateTime dateAction;

    @Column(name = "ip_adresse", length = 50)
    private String ipAdresse;
}
