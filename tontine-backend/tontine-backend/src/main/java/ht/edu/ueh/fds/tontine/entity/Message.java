package ht.edu.ueh.fds.tontine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "MESSAGE", indexes = {
        @Index(name = "idx_message_sol", columnList = "sol_id"),
        @Index(name = "idx_message_prive", columnList = "expediteur_user_id, destinataire_user_id"),
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expediteur_user_id", nullable = false)
    private Utilisateur expediteur;

    @Column(name = "sol_id", length = 36)
    private String solId;

    @Column(name = "destinataire_user_id", length = 36)
    private String destinataireId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contenu;

    @Column(name = "piece_jointe_url", length = 512)
    private String pieceJointeUrl;

    @Column(name = "type_piece", length = 20)
    private String typePiece;

    @CreationTimestamp
    @Column(name = "date_envoi", nullable = false, updatable = false)
    private LocalDateTime dateEnvoi;
}