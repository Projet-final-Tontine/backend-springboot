package ht.edu.ueh.fds.tontine.repository;

import ht.edu.ueh.fds.tontine.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    /** Notifications d'un utilisateur, les plus recentes d'abord. */
    List<Notification> findByUtilisateurIdOrderByDateEnvoiDesc(String utilisateurId);

    /** Notifications non lues d'un utilisateur (badge sur l'app mobile). */
    List<Notification> findByUtilisateurIdAndLueFalse(String utilisateurId);

    long countByUtilisateurIdAndLueFalse(String utilisateurId);
}
