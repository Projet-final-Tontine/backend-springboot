package ht.edu.ueh.fds.tontine.repository;

import ht.edu.ueh.fds.tontine.entity.Tour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TourRepository extends JpaRepository<Tour, String> {

    /** Tous les tours d'un Sol, dans l'ordre du cycle. */
    List<Tour> findBySolIdOrderByNumeroTourAsc(String solId);

    /** Un tour precis d'un Sol. */
    Optional<Tour> findBySolIdAndNumeroTour(String solId, Integer numeroTour);

    /** Le tour actuellement ouvert d'un Sol (collecte en cours). */
    Optional<Tour> findBySolIdAndStatut(String solId, String statut);

    /** Tours dont l'echeance approche (pour les rappels automatiques). */
    List<Tour> findByStatutAndDatePrevueBetween(String statut, LocalDate debut, LocalDate fin);

    /** Les tours ou un utilisateur est beneficiaire. */
    List<Tour> findByBeneficiaireId(String beneficiaireUserId);
}
