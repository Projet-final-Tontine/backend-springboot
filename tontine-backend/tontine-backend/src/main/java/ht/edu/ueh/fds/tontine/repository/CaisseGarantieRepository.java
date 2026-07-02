package ht.edu.ueh.fds.tontine.repository;

import ht.edu.ueh.fds.tontine.entity.CaisseGarantie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CaisseGarantieRepository extends JpaRepository<CaisseGarantie, String> {

    /** La caisse de garantie d'un Sol (une seule par Sol). */
    Optional<CaisseGarantie> findBySolId(String solId);

    boolean existsBySolId(String solId);
}
