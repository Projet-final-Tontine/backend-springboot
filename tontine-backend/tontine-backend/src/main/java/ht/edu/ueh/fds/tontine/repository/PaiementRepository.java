package ht.edu.ueh.fds.tontine.repository;

import ht.edu.ueh.fds.tontine.entity.Paiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, String> {

    /** Historique des paiements d'un utilisateur. */
    List<Paiement> findByUtilisateurIdOrderByDatePaiementDesc(String utilisateurId);

    /** Les paiements rattaches a une cotisation. */
    List<Paiement> findByCotisationId(String cotisationId);

    /** Retrouver un paiement par sa reference Mon Cash (rapprochement). */
    Optional<Paiement> findByReferenceTransaction(String referenceTransaction);

    /** Paiements en attente de validation par la Manman sol. */
    List<Paiement> findByStatutPaiement(String statutPaiement);
}
