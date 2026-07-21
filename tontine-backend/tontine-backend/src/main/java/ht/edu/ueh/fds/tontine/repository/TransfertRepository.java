package ht.edu.ueh.fds.tontine.repository;

import ht.edu.ueh.fds.tontine.entity.Transfert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Accès aux transferts entre utilisateurs. */
public interface TransfertRepository extends JpaRepository<Transfert, String> {

    Optional<Transfert> findByReference(String reference);

    boolean existsByReference(String reference);

    /** Historique d'un utilisateur : transferts envoyés OU reçus, du plus récent. */
    @Query("SELECT t FROM Transfert t WHERE t.expediteurId = :uid OR t.beneficiaireId = :uid "
            + "ORDER BY t.dateCreation DESC")
    List<Transfert> historique(@Param("uid") String utilisateurId);
}
