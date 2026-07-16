package ht.edu.ueh.fds.tontine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * Vote d'un membre sur une demande de secours (pour ou contre).
 * Un seul vote par personne et par demande.
 */
@Entity
@Table(name = "VOTE_SEKOU", uniqueConstraints = {
        @UniqueConstraint(name = "uq_vote_sekou", columnNames = {"demande_id", "votant_user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoteSekou {

    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    @Column(name = "demande_id", nullable = false, length = 36)
    private String demandeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "votant_user_id", nullable = false)
    private Utilisateur votant;

    /** true = pour l'aide, false = contre. */
    @Column(nullable = false)
    private boolean pour;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;
}
