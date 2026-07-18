package ht.edu.ueh.fds.tontine.repository;

import ht.edu.ueh.fds.tontine.entity.BlocRegistre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Accès aux blocs du Registre Inviolable, ordonnés par leur position dans la chaîne. */
public interface BlocRegistreRepository extends JpaRepository<BlocRegistre, String> {

    /** Dernier bloc scellé (le maillon auquel on rattache le prochain). */
    Optional<BlocRegistre> findTopByOrderByPositionDesc();

    /** Toute la chaîne, du plus ancien au plus récent (ordre de vérification). */
    List<BlocRegistre> findAllByOrderByPositionAsc();

    /** Chaîne du plus récent au plus ancien (affichage mobile). */
    List<BlocRegistre> findAllByOrderByPositionDesc();

    long count();
}
