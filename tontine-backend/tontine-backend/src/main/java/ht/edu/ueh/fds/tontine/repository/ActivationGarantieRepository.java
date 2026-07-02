package ht.edu.ueh.fds.tontine.repository;

import ht.edu.ueh.fds.tontine.entity.ActivationGarantie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivationGarantieRepository extends JpaRepository<ActivationGarantie, String> {

    /** Historique des secours declenches pour une caisse donnee. */
    List<ActivationGarantie> findByCaisseGarantieId(String caisseGarantieId);

    /** Secours non rembourses par le membre defaillant (suivi des dettes). */
    List<ActivationGarantie> findByRembourseFalse();

    /** Historique des defaillances d'un membre. */
    List<ActivationGarantie> findByMembreDefaillantId(String membreSolId);
}
