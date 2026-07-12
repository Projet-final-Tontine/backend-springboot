package ht.edu.ueh.fds.tontine.repository;

import ht.edu.ueh.fds.tontine.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {

    /**
     * Messages recents destines a l'utilisateur (pour les notifications) :
     * ses messages prives recus + les messages de groupe de ses Sols, envoyes
     * apres {@code depuis} et par quelqu'un d'autre que lui.
     */
    @Query("""
            SELECT m FROM Message m
            WHERE m.dateEnvoi > :depuis
              AND m.expediteur.id <> :userId
              AND (m.destinataireId = :userId OR m.solId IN :solIds)
            ORDER BY m.dateEnvoi ASC
            """)
    List<Message> messagesRecents(@Param("userId") String userId,
                                  @Param("solIds") Collection<String> solIds,
                                  @Param("depuis") LocalDateTime depuis);

    /** Messages du chat de groupe d'un Sol, du plus ancien au plus recent. */
    List<Message> findBySolIdOrderByDateEnvoiAsc(String solId);

    /** Conversation privee entre deux utilisateurs (dans les deux sens). */
    @Query("""
            SELECT m FROM Message m
            WHERE m.solId IS NULL
              AND ((m.expediteur.id = :a AND m.destinataireId = :b)
                OR (m.expediteur.id = :b AND m.destinataireId = :a))
            ORDER BY m.dateEnvoi ASC
            """)
    List<Message> conversationPrivee(@Param("a") String a, @Param("b") String b);
}
