package ht.edu.ueh.fds.tontine.repository;

import ht.edu.ueh.fds.tontine.entity.TransactionPortefeuille;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionPortefeuilleRepository
        extends JpaRepository<TransactionPortefeuille, String> {

    /** Historique d'un portefeuille, du plus recent au plus ancien. */
    List<TransactionPortefeuille> findByPortefeuilleIdOrderByDateCreationDesc(String portefeuilleId);
}
