package ht.edu.ueh.fds.tontine.repository;

import ht.edu.ueh.fds.tontine.entity.DemandeSekou;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemandeSekouRepository extends JpaRepository<DemandeSekou, String> {

    List<DemandeSekou> findBySolIdOrderByDateCreationDesc(String solId);

    boolean existsByDemandeurIdAndSolIdAndStatut(String demandeurId, String solId, String statut);
}
