package ht.edu.ueh.fds.tontine.repository;

import ht.edu.ueh.fds.tontine.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    /** Trace des actions d'un utilisateur (transparence / audit). */
    List<AuditLog> findByUtilisateurIdOrderByDateActionDesc(String utilisateurId);

    /** Recherche par type d'action (ex : VALIDATION_PAIEMENT). */
    List<AuditLog> findByActionOrderByDateActionDesc(String action);
}
