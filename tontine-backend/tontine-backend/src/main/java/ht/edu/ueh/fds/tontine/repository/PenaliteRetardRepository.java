package ht.edu.ueh.fds.tontine.repository;

import ht.edu.ueh.fds.tontine.entity.PenaliteRetard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PenaliteRetardRepository extends JpaRepository<PenaliteRetard, String> {

    /** Penalites liees a une cotisation. */
    List<PenaliteRetard> findByCotisationId(String cotisationId);

    /** Penalites impayees (suivi de recouvrement). */
    List<PenaliteRetard> findByPayeeFalse();
}
