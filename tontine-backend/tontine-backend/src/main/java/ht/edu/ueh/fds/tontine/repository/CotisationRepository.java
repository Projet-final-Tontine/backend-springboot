package ht.edu.ueh.fds.tontine.repository;

import ht.edu.ueh.fds.tontine.entity.Cotisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface CotisationRepository extends JpaRepository<Cotisation, String> {

    /** Historique des cotisations d'un membre dans un Sol (consulter son compte). */
    List<Cotisation> findByMembreSolIdOrderByDateEcheanceDesc(String membreSolId);

    /** Toutes les cotisations d'un Sol. */
    List<Cotisation> findBySolId(String solId);

    /** Les cotisations d'un tour donne (pour cloturer le tour). */
    List<Cotisation> findByTourId(String tourId);

    /** Cotisations en attente de validation par la Manman sol. */
    List<Cotisation> findBySolIdAndStatut(String solId, String statut);

    /** Cotisations en retard (echeance depassee et non validees). */
    List<Cotisation> findByStatutAndDateEcheanceBefore(String statut, LocalDate date);

    /** Total collecte et valide pour un tour (le montant de la « main »). */
    @Query("SELECT COALESCE(SUM(c.montantPaye), 0) FROM Cotisation c WHERE c.tour.id = :tourId AND c.statut = 'VALIDE'")
    BigDecimal totalValidePourTour(@Param("tourId") String tourId);

    long countByTourIdAndStatut(String tourId, String statut);
}
