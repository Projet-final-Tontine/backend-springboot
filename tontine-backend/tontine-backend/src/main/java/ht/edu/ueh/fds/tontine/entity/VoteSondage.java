package ht.edu.ueh.fds.tontine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * Vote d'un utilisateur pour une option d'un sondage. Un seul vote par personne
 * et par sondage (contrainte d'unicite). Mappe la table {@code VOTE_SONDAGE}.
 */
@Entity
@Table(name = "VOTE_SONDAGE", uniqueConstraints = {
        @UniqueConstraint(name = "uq_vote_sondage_user", columnNames = {"sondage_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoteSondage {

    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    @Column(name = "sondage_id", nullable = false, length = 36)
    private String sondageId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String utilisateurId;

    @Column(name = "option_index", nullable = false)
    private Integer optionIndex;

    @CreationTimestamp
    @Column(name = "date_vote", nullable = false, updatable = false)
    private LocalDateTime dateVote;
}
