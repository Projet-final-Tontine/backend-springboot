package ht.edu.ueh.fds.tontine.repository;

import ht.edu.ueh.fds.tontine.entity.JetonReinitialisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JetonReinitialisationRepository extends JpaRepository<JetonReinitialisation, String> {

    /** Verifier un code de reinitialisation encore valide (non utilise). */
    Optional<JetonReinitialisation> findByCodeAndUtiliseFalse(String code);

    /** Le dernier jeton emis pour un utilisateur. */
    Optional<JetonReinitialisation> findFirstByUtilisateurIdOrderByDateCreationDesc(String utilisateurId);
}
