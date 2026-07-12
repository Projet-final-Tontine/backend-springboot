package ht.edu.ueh.fds.tontine.repository;

import ht.edu.ueh.fds.tontine.entity.VoteSondage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoteSondageRepository extends JpaRepository<VoteSondage, String> {

    /** Tous les votes d'un sondage (pour compter les resultats). */
    List<VoteSondage> findBySondageId(String sondageId);

    /** Le vote d'un utilisateur precis pour un sondage. */
    Optional<VoteSondage> findBySondageIdAndUtilisateurId(String sondageId, String utilisateurId);
}
