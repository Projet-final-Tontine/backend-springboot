package ht.edu.ueh.fds.tontine.repository;

import ht.edu.ueh.fds.tontine.entity.MembreSol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembreSolRepository extends JpaRepository<MembreSol, String> {

    /** Tous les membres d'un Sol, dans l'ordre de la rotation. */
    List<MembreSol> findBySolIdOrderByOrdrePassageAsc(String solId);

    /** Tous les Sols auxquels participe un utilisateur. */
    List<MembreSol> findByUtilisateurId(String utilisateurId);

    /** La participation d'un utilisateur precis dans un Sol precis. */
    Optional<MembreSol> findByUtilisateurIdAndSolId(String utilisateurId, String solId);

    boolean existsByUtilisateurIdAndSolId(String utilisateurId, String solId);

    /**
     * Vrai si l'utilisateur participe au Sol avec un statut precis (ex : ACTIF).
     * Sert au controle d'acces : un membre REFUSE ou EN_ATTENTE ne doit pas
     * pouvoir lire le chat, voter ou consulter le detail comme un membre actif.
     */
    boolean existsByUtilisateurIdAndSolIdAndStatutMembre(
            String utilisateurId, String solId, String statutMembre);

    /** Nombre de places occupees dans un Sol (pour verifier nombre_max_membres). */
    long countBySolId(String solId);

    /** Nombre de membres d'un Sol dans un statut donne (ex : ACTIF). */
    long countBySolIdAndStatutMembre(String solId, String statutMembre);

    /** Membres defaillants d'un Sol (pour l'alerte impaye). */
    List<MembreSol> findBySolIdAndStatutMembre(String solId, String statutMembre);
}
