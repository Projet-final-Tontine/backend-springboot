package ht.edu.ueh.fds.tontine.repository;

import ht.edu.ueh.fds.tontine.entity.Sondage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SondageRepository extends JpaRepository<Sondage, String> {

    /** Sondages d'un Sol, du plus recent au plus ancien. */
    List<Sondage> findBySolIdOrderByDateCreationDesc(String solId);
}
